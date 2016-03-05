package com.mygdx.game.desktop;

import org.opencv.core.Core;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.mygdx.game.Handin4;

public class DesktopLauncher {
	public static void main (String[] arg) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		new LwjglApplication(new Handin4(), config);
	}
}
