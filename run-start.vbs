Set WshShell = CreateObject("WScript.Shell")
WshShell.Run chr(34) & "go.bat" & Chr(34), 1, false
Set WshShell = Nothing
