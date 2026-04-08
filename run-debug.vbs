Set WshShell = CreateObject("WScript.Shell")
WshShell.Run chr(34) & "debug-start.bat" & Chr(34), 1, false
Set WshShell = Nothing
