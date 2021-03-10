# Audio

This guide will explain how to add Background Music & Sound Effects to the project.

## Sound Effects & Background Music
- To add a Sound Effect:
	- Add `AudioSource` to your `GameObject`.
	- Then set the `SoundChannel` and `filename` like this:
	```  java
	audioSourceInstanceName.filename = "filename.wav";
	audioSourceInstanceName.channel = SoundType.SFX;
	```
	- Then to play the Sound Effect use this command:
	``` java
	audioSourceInstanceName.play();
	```
	This will play the Sound Effect
	
- To add some Background Music which will loop continuously:
	- Add `AudioSource` to your `GameObject`.
	- Then set the `SoundChannel` and `filename` like this:
	```  java
	audioSourceInstanceName.filename = "filename.wav";
	audioSourceInstanceName.channel = SoundType.BACKGROUND;
	```
	- Then to play the Background use this command:
	``` java
	audioSourceInstanceName.play();
	```
	This will play the Background

## Change Master Volume & Mute

- To get the current master volume for the Background music use ``AudioManager.getVolume(SoundType.Background);`` and for Sound Effects use ``AudioManager.getVolume(SoundType.SFX);``

- To get the current master mute value for Background music use ``AudioManager.getMute(SoundType.Background);`` and for Sound Effects use ``AudioManager.getMute(SoundType.SFX);``

- To set the current master volume do ``AudioManager.setVolume(SoundChannel.channel, int volumeBetween0and100)``

- To set the current master mute do ``AudioManager.setMute(SoundChannel.channel, boolean muteisTrue)``

## Sound Files.

- This audio player only works with .wav (also with AIFF or AU but I have not tested these).  All sounds MUST be like this file type
- At the moment please put them in the resources/audio folder (This is dependent on audioFix being pushed in which will be sorted ASAP)