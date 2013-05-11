package Reika.DragonAPI;

import javax.sound.midi.Sequence;

public final class MIDIInterface {

	private final Sequence midi;

	public MIDIInterface(Class root, String path, String back) {
		midi = ReikaMIDIReader.getMIDIFromFile(root, path, back);
	}

	/** Returns the note at the given track and time. Args: Track, Time */
	public int getNoteAtTrackAndTime(int track, int time) {/*
		int a = ReikaMIDIReader.readMIDI(midi, track, time, 0);
		if (a != 0 && a != -1)
		;//ReikaJavaLibrary.pConsole(time+" @ "+a);
		return a;*/
		//return ReikaMIDIReader.getMidiNoteAtChannelAndTime(midi, time, track);
		return 0;
	}

	public void debug() {
		ReikaMIDIReader.debugMIDI(midi);
	}

	public int getLength() {
		return ReikaMIDIReader.getMidiLength(midi);
	}

	public int[][][] fill() {
		return ReikaMIDIReader.readMIDIFile(midi);
	}

}
