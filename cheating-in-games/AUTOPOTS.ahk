; Potting script for online games. Spams potion key when hp is at ~20% or below

;look at pixel defined by range x=(150,151) y=(84,85) and pass color 0xFFFFF with a margin of 10
;if the pixel is in the right range, ErrorLevel=0, which means that the health bar has reached a low value
while 1=1 {
PixelSearch, OutputVarX, OutputVarY, 150, 84, 151, 85, 0xFFFFFF, 10, Fast
if ErrorLevel=0
{
	loop {
		Send {z}
		Sleep 70
		Send {z}
		Sleep 60
		Send {z}
		Sleep 60
		Reload
	}
}
}
[::Pause
F9::ExitApp
Home::Reload
return