package audio;

import javax.sound.sampled.*;
import java.io.File;

/**
 * <p>A simple system for playing sounds in the clients currently just for events
 * easy to expand to handle the background music too.
 * Each client will create an instance of this class to control audio</p>
 */
public enum Sounds {
	// The filepaths of the sounds
	intro ("files/pacman_beginning.wav"),
	chomp ("files/pacman_chomp.wav"),
	death ("files/pacman_death.wav");
	private final String path;
	private Boolean mute;
	private Clip music;
	
	public void setMusicVolume(double musicVolume) {
		this.musicVolume=musicVolume;
		refreshMusic();
	}
	
	public void setSoundVolume(double soundVolume) {
		this.soundVolume=soundVolume;
	}
	
	private double musicVolume;
	private double soundVolume;
	
	Sounds(String path){
		this.path = path;
		mute = false;
		music = null;
		musicVolume = 0.5;
		soundVolume = 0.5;
	}
	
	/**
	 * <p>This toggles the mute boolean on and off to be called
	 * by the ui when a mute button is pressed (can become a Setter</p>
	 */
	public void toggleMute(){
		mute = !mute;
		refreshMusic();
	}
	
	private void refreshMusic() {
		if(music == null) return;
		setClipVolume(music, mute ? 0 : musicVolume);
	}
	
	/**
	 * <p>This plays the given sound</p>
	 * @param sound the sound to play
	 */
	public void playSound(Sounds sound) {
		if(mute) return; // IF the client has muted its audio nothing will be played
		File audioFile = new File(sound.path);
		try {
			AudioInputStream stream=AudioSystem.getAudioInputStream(audioFile);
			DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat());
			Clip clip = (Clip) AudioSystem.getLine(info);
			clip.open(stream);
			setClipVolume(clip, soundVolume);
			clip.start();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * <p>Sets the volume of the given clip</p>
	 * @param clip The clip to set the volume of
	 * @param volume the volume (between 0.0 and 1.0)
	 */
	public void setClipVolume(Clip clip, double volume) {
		if(clip == null) {
			// Change to logging once setup
			System.err.println(" null clip sent to set volume");
			return;
		}
		if(volume > 1.0) volume = 1.0;
		if(volume < 0.0) volume = 0.0;
		FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		control.setValue((float) (Math.log(volume) / Math.log(10.0) * 20.0));
	}
	/**
	 * <p>This plays the given sound as the background music (playing it until told to stop or play other music)</p>
	 * @param sound file for the music
	 */
	public void playMusic(Sounds sound) {
		File audioFile = new File(sound.path);
		try {
			AudioInputStream stream=AudioSystem.getAudioInputStream(audioFile);
			DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat());
			Clip clip = (Clip) AudioSystem.getLine(info);
			clip.open(stream);
			if(music != null){
				music.stop();
				music.close();
			}
			music = clip;
			setClipVolume(music, musicVolume);
			music.loop(Clip.LOOP_CONTINUOUSLY);
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	
}
