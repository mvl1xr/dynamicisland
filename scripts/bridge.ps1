# Windows Bridge for Dynamic Island: Media, Notifications & Network
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$cacheDir = Join-Path $PSScriptRoot "..\.cache"
if (-not (Test-Path $cacheDir)) {
    New-Item -ItemType Directory -Path $cacheDir -Force | Out-Null
}

$pinvoke = @"
using System;
using System.IO;
using System.Runtime.InteropServices;
using System.Runtime.InteropServices.ComTypes;

public static class NativeStreamHelper {
    [DllImport("shcore.dll")]
    public static extern int CreateStreamOverRandomAccessStream(
        [MarshalAs(UnmanagedType.IUnknown)] object randomAccessStream,
        ref Guid riid,
        out IStream ppv);

    public static byte[] ReadRandomAccessStream(object comObj) {
        if (comObj == null) return null;
        Guid iidIStream = new Guid("0000000c-0000-0000-C000-000000000046");
        IStream stream;
        int hr = CreateStreamOverRandomAccessStream(comObj, ref iidIStream, out stream);
        if (hr != 0 || stream == null) return null;

        using (MemoryStream ms = new MemoryStream()) {
            byte[] buffer = new byte[8192];
            IntPtr pcbRead = Marshal.AllocHGlobal(sizeof(int));
            try {
                while (true) {
                    stream.Read(buffer, buffer.Length, pcbRead);
                    int read = Marshal.ReadInt32(pcbRead);
                    if (read <= 0) break;
                    ms.Write(buffer, 0, read);
                }
            } finally {
                Marshal.FreeHGlobal(pcbRead);
            }
            return ms.ToArray();
        }
    }
}
"@
Add-Type -TypeDefinition $pinvoke -ErrorAction SilentlyContinue

[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media, ContentType=WindowsRuntime] | Out-Null
[Windows.UI.Notifications.Management.UserNotificationListener, Windows.UI, ContentType=WindowsRuntime] | Out-Null
Add-Type -AssemblyName System.Runtime.WindowsRuntime -ErrorAction SilentlyContinue

$asTaskGeneric = [System.WindowsRuntimeSystemExtensions].GetMethods() | ? { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' }
function AwaitTask($asyncOp, $type) {
    if ($asyncOp -eq $null) { return $null }
    $m = $asTaskGeneric.MakeGenericMethod($type)
    $netTask = $m.Invoke($null, @($asyncOp))
    $netTask.Wait(4000) | Out-Null
    return $netTask.Result
}

# Request GSMTC Manager
$mgr = $null
try {
    $mgrOp = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()
    $mgr = AwaitTask $mgrOp ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
} catch {}

# Request Notification Listener
$listener = $null
try {
    $listener = [Windows.UI.Notifications.Management.UserNotificationListener]::Current
} catch {}

$lastMediaKey = ""
$lastWifiLevel = -1
$seenNotifIds = New-Object 'System.Collections.Generic.HashSet[int]'

# Temporarily suppress Windows native toast sound so only Apple ding.mp3 plays
$regSoundPath = "HKCU:\AppEvents\Schemes\Apps\.Default\Notification.Default\.Current"
$origSound = $null
try {
    $origSound = (Get-ItemProperty -Path $regSoundPath -ErrorAction SilentlyContinue).'(Default)'
    Set-ItemProperty -Path $regSoundPath -Name "(Default)" -Value "" -ErrorAction SilentlyContinue
} catch {}

# Seed existing notifications so we don't display old notifications on startup
try {
    if ($listener) {
        $existing = AwaitTask ($listener.GetNotificationsAsync([Windows.UI.Notifications.NotificationKinds]::Toast)) ([System.Collections.Generic.IReadOnlyList[Windows.UI.Notifications.UserNotification]])
        if ($existing) {
            foreach ($item in $existing) {
                $seenNotifIds.Add($item.Id) | Out-Null
            }
        }
    }
} catch {}

Write-Host "BRIDGE_READY"

$coverPath = Join-Path $cacheDir "cover.jpg"
$logoPath = Join-Path $cacheDir "notif.png"

$ping = New-Object System.Net.NetworkInformation.Ping

while ($true) {
    # 1. Check Media
    try {
        if ($mgr) {
            $session = $mgr.GetCurrentSession()
            if ($session) {
                $info = $session.GetPlaybackInfo()
                $isPlaying = ($info -and $info.PlaybackStatus -eq [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus]::Playing)
                $prop = AwaitTask ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
                
                $title = if ($prop -and $prop.Title) { $prop.Title } else { "" }
                $artist = if ($prop -and $prop.Artist) { $prop.Artist } else { "" }
                $mediaKey = "$title|$artist|$isPlaying"

                if ($mediaKey -ne $lastMediaKey) {
                    $lastMediaKey = $mediaKey
                    $hasCover = $false
                    if ($prop -and $prop.Thumbnail) {
                        try {
                            $thumbStream = AwaitTask ($prop.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
                            $bytes = [NativeStreamHelper]::ReadRandomAccessStream($thumbStream)
                            if ($bytes -and $bytes.Length -gt 0) {
                                [System.IO.File]::WriteAllBytes($coverPath, $bytes)
                                $hasCover = $true
                            }
                        } catch {}
                    }
                    
                    $mediaJson = @{
                        type = "media"
                        title = $title
                        artist = $artist
                        isPlaying = $isPlaying
                        coverPath = if ($hasCover) { $coverPath } else { "" }
                    } | ConvertTo-Json -Compress
                    Write-Host "EVENT:$mediaJson"
                }
            } else {
                if ($lastMediaKey -ne "NONE") {
                    $lastMediaKey = "NONE"
                    Write-Host 'EVENT:{"type":"media","title":"","artist":"","isPlaying":false,"coverPath":""}'
                }
            }
        }
    } catch {}

    # 2. Check Notifications
    try {
        if ($listener) {
            $notifs = AwaitTask ($listener.GetNotificationsAsync([Windows.UI.Notifications.NotificationKinds]::Toast)) ([System.Collections.Generic.IReadOnlyList[Windows.UI.Notifications.UserNotification]])
            if ($notifs -and $notifs.Count -gt 0) {
                foreach ($latest in $notifs) {
                    if (-not $seenNotifIds.Contains($latest.Id)) {
                        $seenNotifIds.Add($latest.Id) | Out-Null
                        if ($seenNotifIds.Count -gt 500) {
                            $seenNotifIds.Clear()
                        }
                        $appName = $latest.AppInfo.DisplayInfo.DisplayName
                        $titleText = ""
                        $bodyText = ""
                        
                        $binding = $latest.Notification.Visual.GetBinding([Windows.UI.Notifications.KnownNotificationBindings]::ToastGeneric)
                        if ($binding) {
                            $elements = $binding.GetTextElements()
                            if ($elements.Count -gt 0) { $titleText = $elements[0].Text }
                            if ($elements.Count -gt 1) { $bodyText = $elements[1].Text }
                        }

                        # Immediately remove notification from Windows toast so it only shows in Dynamic Island!
                        try {
                            $listener.RemoveNotification($latest.Id)
                        } catch {}

                        # Telegram detection & silent check
                        $isTelegram = ($appName -like "*Telegram*")
                        $isSilent = $false
                        if ($isTelegram) {
                            if ($titleText -match 'muted|silent' -or $bodyText -match 'muted|silent') {
                                $isSilent = $true
                            }
                            try {
                                if ($latest.Notification.SuppressPopup -eq $true) {
                                    $isSilent = $true
                                }
                            } catch {}
                        }

                        # Discord detection & mention check
                        $isDiscord = ($appName -like "*Discord*")
                        $isMention = $false
                        if ($isDiscord) {
                            if ($titleText -match '@' -or $bodyText -match '@' -or $titleText -match '\(DM\)' -or $titleText -notmatch '#') {
                                $isMention = $true
                            }
                        }

                        $hasLogo = $false
                        try {
                            $logoRef = $latest.AppInfo.DisplayInfo.GetLogo([Windows.Foundation.Size]::new(32, 32))
                            if ($logoRef) {
                                $logoStream = AwaitTask ($logoRef.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
                                $logoBytes = [NativeStreamHelper]::ReadRandomAccessStream($logoStream)
                                if ($logoBytes -and $logoBytes.Length -gt 0) {
                                    [System.IO.File]::WriteAllBytes($logoPath, $logoBytes)
                                    $hasLogo = $true
                                }
                            }
                        } catch {}

                        $notifJson = @{
                            type = "notification"
                            appName = $appName
                            title = $titleText
                            text = $bodyText
                            iconPath = if ($hasLogo) { $logoPath } else { "" }
                            isTelegram = $isTelegram
                            isSilent = $isSilent
                            isDiscord = $isDiscord
                            isMention = $isMention
                        } | ConvertTo-Json -Compress
                        Write-Host "EVENT:$notifJson"
                    }
                }
            }
        }
    } catch {}

    # 3. Check Network
    try {
        $wifiLevel = 1
        $connected = [System.Net.NetworkInformation.NetworkInterface]::GetIsNetworkAvailable()
        if (-not $connected) {
            $wifiLevel = 4
        } else {
            # Quick check latency
            try {
                $reply = $ping.Send("1.1.1.1", 300)
                if ($reply.Status -eq [System.Net.NetworkInformation.IPStatus]::Success) {
                    if ($reply.RoundtripTime -lt 80) { $wifiLevel = 1 }
                    elseif ($reply.RoundtripTime -lt 160) { $wifiLevel = 2 }
                    else { $wifiLevel = 3 }
                } else {
                    $wifiLevel = 3
                }
            } catch {
                $wifiLevel = 2
            }
        }

        if ($wifiLevel -ne $lastWifiLevel) {
            $lastWifiLevel = $wifiLevel
            $netJson = @{
                type = "network"
                wifiLevel = $wifiLevel
                connected = $connected
            } | ConvertTo-Json -Compress
            Write-Host "EVENT:$netJson"
        }
    } catch {}

    Start-Sleep -Milliseconds 150
}
