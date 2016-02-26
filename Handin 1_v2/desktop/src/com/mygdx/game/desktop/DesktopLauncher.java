package com.mygdx.game.desktop;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.mygdx.game.Handin2;
import com.mygdx.game.Handin2Part2;
import com.mygdx.game.Handin3;
import com.mygdx.game.Handin3part2;
import com.mygdx.game.Handin3part3;
import com.mygdx.game.MyGdxGame1;

public class DesktopLauncher {
	public static void main (String[] arg) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		new LwjglApplication(new Handin3part3(), config);
	}
}
