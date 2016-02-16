package com.mygdx.game;

import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

public class Handin2 extends ApplicationAdapter {
	
	public PerspectiveOffCenterCamera libGdxCam;
	public Environment environment;
    public ModelBatch modelBatch;
    public ModelInstance boxInstances[];
	public Model boxModel;
	Mat cameraMatrix;
	
	VideoCapture camera;
	MatOfPoint3f chessboard3dPos;
	
	boolean isCalibrated = false;
	List<Mat> objectPoints;
	List<Mat> imagePoints;
	private Size calibChessboardSize = new Size(9,6);
	private int calibCounter = 0;
	
	
	@Override
	public void create() {
		objectPoints = new ArrayList<Mat>();
		imagePoints = new ArrayList<Mat>();
		
		libGdxCam = new PerspectiveOffCenterCamera();
		cameraMatrix = UtilAR.getDefaultIntrinsics(640f, 480f);
		libGdxCam.setByIntrinsics(cameraMatrix, 640f, 480f);
		libGdxCam.update();
		
		Point3[] positions = new Point3[49];
		for (int i=0; i<7; i++) {
			for (int j=0; j<7; j++) {
				positions[i*7+j] = new Point3(i+1, j+1, 0);
			}
		}
		
		camera = new VideoCapture(0);
		chessboard3dPos = new MatOfPoint3f(positions);
		

		environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
		
        modelBatch = new ModelBatch();
        
		ModelBuilder modelBuilder = new ModelBuilder();
        boxModel = modelBuilder.createBox(1f, 1f, 1f, 
            new Material(ColorAttribute.createDiffuse(Color.RED)),
            Usage.Position | Usage.Normal);
		
        boxInstances = new ModelInstance[32];
        for (int i=0; i<8; i++) {
        	for (int j=0; j<8; j=j+2) {
        		int k = j;
        		if (i%2 == 1) {
        			k++;
        		}
        		ModelInstance instance = new ModelInstance(boxModel); 
        		instance.transform.translate(i+0.5F, k+0.5F, 0.5F);
	        	boxInstances[i*4+j/2] = instance;
        	}
        }
        
        
        
	}
	
	@Override
	public void render() {
		Mat cameraImage = new Mat();
		camera.read(cameraImage);
		
		Mat greyImage = new Mat();
		Imgproc.cvtColor(cameraImage, greyImage, Imgproc.COLOR_BGR2GRAY);
		
		MatOfPoint2f corners = new MatOfPoint2f();
		corners.alloc(49);
		boolean found = Calib3d.findChessboardCorners(greyImage, new Size(7, 7),
				corners, Calib3d.CALIB_CB_ADAPTIVE_THRESH);
		
		if (! isCalibrated) {
			
			calibCounter++;
			if (calibCounter > 20) {
				Mat cameraMatrix = new Mat();
				Mat distCoeffs = new Mat();
				List<Mat> rvecs = new ArrayList<Mat>();
				List<Mat> tvecs = new ArrayList<Mat>();
				
				Calib3d.calibrateCamera(objectPoints, imagePoints, calibChessboardSize,
						cameraMatrix, distCoeffs, rvecs, tvecs);
				isCalibrated = true;
			}
		}
		
		if (found) {
			Calib3d.drawChessboardCorners(cameraImage, new Size(7,7), corners, true);
			
			Mat rvec = new Mat();
			Mat tvec = new Mat();
			Calib3d.solvePnP(chessboard3dPos, corners, cameraMatrix,
					UtilAR.getDefaultDistortionCoefficients(), rvec, tvec);
			
			UtilAR.setCameraByRT(rvec, tvec, libGdxCam);
		}
		
		
		UtilAR.imDrawBackground(cameraImage);
		
		if (found) {
			modelBatch.begin(libGdxCam);
			for (ModelInstance m : boxInstances) {
				modelBatch.render(m, environment);			
			}
	        modelBatch.end();
		}
	}
	
	@Override
	public void dispose() {
		camera.release();
		modelBatch.dispose();
		boxModel.dispose();
	}
	
}
