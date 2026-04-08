Set WshShell = CreateObject("WScript.Shell")
WshShell.Run chr(34) & "start-all-final.bat" & Chr(34), 1, false
Set WshShell = Nothing
