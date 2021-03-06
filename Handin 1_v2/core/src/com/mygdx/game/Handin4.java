package com.mygdx.game;

import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Handin4 extends ApplicationAdapter {
	
	private static final int GRID_SIZE = 20;
	private static final int NUMBER_OF_MARKERS = 8;
	
	private static final int ID_MARKER_1 = 0;
	private static final int ID_MARKER_2 = 1;
	private static final int ID_MARKER_3 = 2;
	private static final int ID_MARKER_4 = 3;
	private static final int ID_MARKER_RED = 4;
	private static final int ID_MARKER_GREEN = 5;
	private static final int ID_MARKER_BLUE = 6;
	private static final int ID_MARKER_SPEED = 7;
	
	private int[][][] averagedMarkers = new int[NUMBER_OF_MARKERS][GRID_SIZE][GRID_SIZE];
	private static final int homoSize = 200;
	MatOfPoint2f homoPoints;
	
	public PerspectiveOffCenterCamera libGdxCam;
	Mat cameraMatrix;
	VideoCapture camera;
	int cameraResolutionX = 640;
	int cameraResolutionY = 480;
	
	public Environment environment;
    public ModelBatch modelBatch;
    public ModelInstance boxInstance;
    public Model boxModel;
    public Model fullMarkerBoxModel;
    public Model xAxisModel;
    public Model yAxisModel;
    public Model zAxisModel;
    public Array<ModelInstance> instances = new Array<ModelInstance>();
    public float step;
    public int prevMarker = 0;
    public int nextMarker = 0;
    public float x = 0;
    public float y = 0;
    public float z = 0;
    public Material material;
    
	public float x1 = 0;
	public float x2 = 0;
	public float x3 = 0;
	public float x4 = 0;
	public float y1 = 0;
	public float y2 = 0;
	public float y3 = 0;
	public float y4 = 0;
	public float z1 = 0;
	public float z2 = 0;
	public float z3 = 0;
	public float z4 = 0;
	
	public float basespeed = 1;
	public float speed = 1;
	public float sx = 0;
	public float sy = 0;
	public float sz = 0;
	
	@Override
	public void create() {
		String dir = "../core/assets/";
		String[] files = {"point4.png", "point1.png", "point2.png", "point3.png",
				"red.png", "green.png", "blue.png", "speed.png"};
		for (int i=0; i<files.length; i++) {
			String file = dir + files[i];
			Mat marker = Imgcodecs.imread(file);
			Mat greyMarker = new Mat();
			Imgproc.cvtColor(marker, greyMarker, Imgproc.COLOR_BGR2GRAY);	
			Mat binaryMarker = new Mat(greyMarker.size(), greyMarker.type());
			Imgproc.threshold(greyMarker, binaryMarker, 100, 255, Imgproc.THRESH_BINARY);
			averagedMarkers[i] = averagePixels(binaryMarker);
		}
		
		libGdxCam = new PerspectiveOffCenterCamera();
		cameraMatrix = UtilAR.getDefaultIntrinsics(640f, 480f);
		libGdxCam.setByIntrinsics(cameraMatrix, 640f, 480f);
		libGdxCam.far = 1000;
		libGdxCam.position.set(5f, 5f, 5f);
		libGdxCam.lookAt(0,0,0);
		libGdxCam.update();
		
		camera = new VideoCapture(0);
		
		environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
		
        modelBatch = new ModelBatch();
        
		ModelBuilder modelBuilder = new ModelBuilder();
        boxModel = modelBuilder.createBox(1f, 1f, 1f, 
            new Material(ColorAttribute.createDiffuse(Color.BLACK)),
            Usage.Position | Usage.Normal);
        boxInstance = new ModelInstance(boxModel);
        
        fullMarkerBoxModel = modelBuilder.createBox(10f, 10f, 10f, 
        		new Material(ColorAttribute.createDiffuse(Color.WHITE)),
        		Usage.Position | Usage.Normal);
        
        xAxisModel = modelBuilder.createArrow(new Vector3(0,0,0), new Vector3(0,0,-10), 
				 new Material(ColorAttribute.createDiffuse(Color.BLUE)), 
				 Usage.Position | Usage.Normal);
        
	}
	
	@Override
	public void render() {
		
		// Image
		Mat cameraImage = new Mat();
		camera.read(cameraImage);	
		Mat greyImage = new Mat();
		Imgproc.cvtColor(cameraImage, greyImage, Imgproc.COLOR_BGR2GRAY);	
		Mat binaryImage = new Mat(greyImage.size(), greyImage.type());
		Imgproc.threshold(greyImage, binaryImage, 100, 255, Imgproc.THRESH_BINARY);
		
		// Countours
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(binaryImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
		
		// Markers
		ArrayList<MatOfPoint> ap;
		ap = new ArrayList<MatOfPoint>();
		ArrayList<MatOfPoint> results = new ArrayList<MatOfPoint>();
		for (int i=0; i<contours.size(); i++) {
			Point[] contour = contours.get(i).toArray();

			MatOfPoint2f curve = new MatOfPoint2f(contour);
			MatOfPoint2f approxCurve = new MatOfPoint2f();
			Imgproc.approxPolyDP(curve, approxCurve, Imgproc.arcLength(curve, true)*0.02, true);
			if (approxCurve.toList().size() == 4 && Imgproc.contourArea(approxCurve,true) > 2000) {
							
				ap.add(new MatOfPoint(approxCurve.toArray()));	
				results.add(new MatOfPoint(approxCurve.toArray()));
			}
		}
		
		Boolean redMarker = false; 
		Boolean blueMarker = false; 
		Boolean greenMarker = false; 
		Boolean speedMarker = false; 
		Boolean m1Marker = false; 
		Boolean m2Marker = false; 
		Boolean m3Marker = false; 
		Boolean m4Marker = false;
		
		MatOfPoint2f[] sortedMarkerResults = new MatOfPoint2f[NUMBER_OF_MARKERS];
		
		for(int i=0; i<results.size(); i++) {
			MatOfPoint2f imagePoints = new MatOfPoint2f(results.get(i).toArray());
			
			Point[] objPoints = new Point[4];
			objPoints[0] = new Point(0,0);
			objPoints[1] = new Point(homoSize,0);
			objPoints[2] = new Point(homoSize,homoSize);
			objPoints[3] = new Point(0,homoSize);
			homoPoints = new MatOfPoint2f(objPoints);
			
	        //Mat homography = Calib3d.findHomography(imagePoints, homoPoints);
			Mat homography = ourFindHomography(results.get(i).toArray(), objPoints);
			
			
			
			
			
	        Mat rectified = new Mat();
	        Imgproc.warpPerspective(cameraImage, rectified, homography, new Size(homoSize,homoSize));
	        //UtilAR.imShow("key"+i,rectified);
	        
	        int[] match = bestMarkerMatch(rectified);
	        
	        // Cutoff threshold
	        if (match[1] < 80) {
	        	//continue;
	        }
	        //System.out.println("Marker " + match[0] + ": " + match[1]);
	        Imgproc.drawContours(cameraImage, results, i, new Scalar(0,0,255));
	        
	        sortedMarkerResults[match[0]] = imagePoints;
	        switch (match[0]) {
	        	case ID_MARKER_1:
	        		m1Marker = true;
	        		break;
	        	case ID_MARKER_2:
	        		m2Marker = true;
	        		break;
	        	case ID_MARKER_3:
	        		m3Marker = true;
	        		break;
	        	case ID_MARKER_4:
	        		m4Marker = true;
	        		break;
	        	case ID_MARKER_RED:
	        		redMarker = true;
	        		break;
	        	case ID_MARKER_GREEN:
	        		greenMarker = true;
	        		break;
	        	case ID_MARKER_BLUE:
	        		blueMarker = true;
	        		break;
	        	case ID_MARKER_SPEED:
	        		speedMarker = true;
	        		break;
	        }
		}
		
		//ColorZ
		if (redMarker == true && blueMarker == false && greenMarker == false) {
			boxInstance.materials.first().set(new Material(ColorAttribute.createDiffuse(Color.RED)));
		}
		if(redMarker == false && blueMarker == false && greenMarker == true) {
			boxInstance.materials.first().set(new Material(ColorAttribute.createDiffuse(Color.GREEN)));
		}
		if (redMarker == false && blueMarker == true && greenMarker == false) {
			boxInstance.materials.first().set(new Material(ColorAttribute.createDiffuse(Color.BLUE)));
		}
		if(redMarker == true && blueMarker == true && greenMarker == false) {
			boxInstance.materials.first().set(new Material(ColorAttribute.createDiffuse(Color.PURPLE)));
		}
		if(redMarker == true && blueMarker == false && greenMarker == true) {
			boxInstance.materials.first().set(new Material(ColorAttribute.createDiffuse(Color.YELLOW)));
		}
		if(redMarker == false && blueMarker == true && greenMarker == true) {
			boxInstance.materials.first().set(new Material(ColorAttribute.createDiffuse(Color.CYAN)));
		}
		if(redMarker == true && blueMarker == true && greenMarker == true) {
			boxInstance.materials.first().set(new Material(ColorAttribute.createDiffuse(Color.WHITE)));
		}
		if(redMarker == false && blueMarker == false && greenMarker == false) {
			boxInstance.materials.first().set(new Material(ColorAttribute.createDiffuse(Color.BLACK)));
		}
		
		
		//Imgproc.drawContours(cameraImage, ap, 0, new Scalar(0,0,255));
		
		Point3[] objPoints = new Point3[4];
		objPoints[0] = new Point3(-5,-5,0);
		objPoints[1] = new Point3(5,-5,0);
		objPoints[2] = new Point3(5,5,0);
		objPoints[3] = new Point3(-5,5,0);
		MatOfPoint3f objectPoints = new MatOfPoint3f(objPoints);
		
		if (m1Marker) {
			
			// Marker 1 er centrum for verden
			UtilAR.setNeutralCamera(libGdxCam);
			instances = new Array<ModelInstance>();
			
			MatOfPoint2f imagePoints = new MatOfPoint2f(sortedMarkerResults[0].toArray());
			
			Mat rvec = new Mat();
			Mat tvec = new Mat();
			Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix,
					UtilAR.getDefaultDistortionCoefficients(), rvec, tvec);					
			
			Vector3 v = new Vector3(0,0,0);
			Boolean draw = false;
			int markers = 1;
			
			ModelInstance a1 = new ModelInstance(xAxisModel);
			ModelInstance a2, a3, a4;
			a2 = null;
			a3 = null;
			a4 = null;
			
			UtilAR.setTransformByRT(rvec, tvec, a1.transform);
			instances.add(a1);
			Vector3 v1 = a1.transform.getTranslation(v);
			//System.out.println(v1);
			float xx = v1.x;
			float xy = v1.y;
			float xz = v1.z;
			x1 = 0;
			y1 = 0;
			z1 = 0;
			if(m2Marker) {
				markers++;
				imagePoints = new MatOfPoint2f(sortedMarkerResults[1].toArray());
				Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix,
						UtilAR.getDefaultDistortionCoefficients(), rvec, tvec);	
				a2 = new ModelInstance(xAxisModel);
				UtilAR.setTransformByRT(rvec, tvec, a2.transform);
				instances.add(a2);
				Vector3 v2 = a2.transform.getTranslation(v);
				//System.out.println(v1);
				//System.out.println(v2);
				x2 = v2.x - xx;
				y2 = v2.y - xy;
				z2 = v2.z - xz;
			}
			if(m3Marker) {
				markers++;
				imagePoints = new MatOfPoint2f(sortedMarkerResults[2].toArray());
				Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix,
						UtilAR.getDefaultDistortionCoefficients(), rvec, tvec);	
				a3 = new ModelInstance(xAxisModel);
				UtilAR.setTransformByRT(rvec, tvec, a3.transform);
				instances.add(a3);
				Vector3 v3 = a3.transform.getTranslation(v);
				x3 = v3.x - xx;
				y3 = v3.y - xy;
				z3 = v3.z - xz;
			}
			if(m4Marker) {
				markers++;
				imagePoints = new MatOfPoint2f(sortedMarkerResults[3].toArray());
				Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix,
						UtilAR.getDefaultDistortionCoefficients(), rvec, tvec);	
				a4 = new ModelInstance(xAxisModel);
				UtilAR.setTransformByRT(rvec, tvec, a4.transform);
				instances.add(a4);
				Vector3 v4 = a4.transform.getTranslation(v);
				x4 = v4.x - xx;
				y4 = v4.y - xy;
				z4 = v4.z - xz;
			}
			if(nextMarker == 0){
				nextMarker = 1;
				prevMarker = 1;
			}
			if(markers == 1) {
				// Vi har kun en marker, flyv hen imod den.
				// m1 er altid til stede, s� det m� v�re den.
				prevMarker = nextMarker;
				nextMarker = 1;
				draw = true;
				
			}
			else {
				// Flere markers
				// Er vi n�et til destinationen?
				if(reachedDestination()) {
					// Find ny destination.
					setNextMarker(m1Marker, m2Marker, m3Marker, m4Marker);
					draw = true;
				}
				else {
					// Ikke n�et til destinationen.
					draw = true;
				}
			}
			
			if(speedMarker) {
				// Find placeringen af speed
				imagePoints = new MatOfPoint2f(sortedMarkerResults[ID_MARKER_SPEED].toArray());
				Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix,
						UtilAR.getDefaultDistortionCoefficients(), rvec, tvec);	
				ModelInstance a5 = new ModelInstance(xAxisModel);
				UtilAR.setTransformByRT(rvec, tvec, a5.transform);
				Vector3 v5 = a5.transform.getTranslation(v);
				sx = v5.x - xx;
				sy = v5.y - xy;
				sz = v5.z - xz;
				
				double distance = Math.sqrt(Math.pow(sx,2)+Math.pow(sy,2)+Math.pow(sz, 2));
				int factor = (int) (distance / 10);
				if(factor == 0) factor = 1;
				speed = basespeed * factor;
			}
			else {
				speed = basespeed;
			}
			
			
			
			
			//System.out.println("1: " + x1 + ", " + y1 + ", " + z1);
			Vector3 test1 = a1.transform.getTranslation(new Vector3());
			//System.out.println("t1: " + test1.x + ", " + test1.y + ", " + test1.z);
			//System.out.println("2: " + x2 + ", " + y2 + ", " + z2);
			if (m2Marker) {
				Vector3 test2 = a2.transform.getTranslation(new Vector3());
				//System.out.println("t2: " + test2.x + ", " + test2.y + ", " + test2.z);
			}
			//System.out.println("next: " + nextMarker + ", prev: " + prevMarker);
			if(draw) { // Note at med dette setup er draw altid true
				// Incrementer flyets position.
				Vector3 flightVector = getFlightVector();
				//System.out.println("plane: " + x + ", " + y + ", " + z);
				//System.out.println("FlightVector: " + flightVector);
				float total = Math.abs(flightVector.x) + Math.abs(flightVector.y) + Math.abs(flightVector.z);
				if(flightVector.x != 0)	x = x+(speed*(flightVector.x/total)); // Nuv�rende position + proportionelt i retning
				if(flightVector.y != 0) y = y+(speed*(flightVector.y/total));
				if(flightVector.z != 0) z = z+(speed*(flightVector.z/total));
				//System.out.println("plane2: " + x + ", " + y + ", " + z);
				Vector3 fly = new Vector3(x,y,z);
				Vector3 fly2 = new Vector3();
				if(flightVector.x != 0)	fly2.x = (speed*(flightVector.x/total)); // Nuv�rende position + proportionelt i retning
				if(flightVector.y != 0) fly2.y = (speed*(flightVector.y/total));
				if(flightVector.z != 0) fly2.z = (speed*(flightVector.z/total));
				//System.out.println("fly2: " + fly2);
				
			
				// Genfind rvec og tvec for m1
				imagePoints = new MatOfPoint2f(sortedMarkerResults[0].toArray());
				Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix,
						UtilAR.getDefaultDistortionCoefficients(), rvec, tvec);	
				// Placer flyet.
				Vector3 planePos = boxInstance.transform.getTranslation(new Vector3());
				boxInstance.transform.translate(-planePos.x, -planePos.y, -planePos.z);
				Vector3 newPlanePos = new Vector3(fly.x + xx, fly.y + xy, fly.z + xz);
				boxInstance.transform.translate(newPlanePos);
				/*
				System.out.println("box1 " + boxInstance.transform.getTranslation(new Vector3()));
				UtilAR.setTransformByRT(rvec, tvec, boxInstance.transform);
				System.out.println("box2 " + boxInstance.transform.getTranslation(new Vector3()));
				//System.out.println(fly.x + ", " + fly.y + ", " + fly.z);
				System.out.println("fly: " + fly);
				boxInstance.transform.translate(fly);
				System.out.println("box3 " + boxInstance.transform.getTranslation(new Vector3()));
				*/
				instances.add(boxInstance);
			}
		
			UtilAR.imDrawBackground(cameraImage);
			modelBatch.begin(libGdxCam);
			modelBatch.render(instances, environment);
			modelBatch.end();
	        
		} else {
			UtilAR.imDrawBackground(cameraImage);
		}	
	}

	@Override
	public void dispose() {
		camera.release();
		modelBatch.dispose();
		boxModel.dispose();
	}
	
	public void setNextMarker(boolean m1Marker, boolean m2Marker, boolean m3Marker, boolean m4Marker) {
		if(nextMarker == 1) {
			if(m2Marker) {
				nextMarker = 2;
				prevMarker = 1;
			}
			else if(m3Marker) {
				nextMarker = 3;
				prevMarker = 1;
			}
			else {
				nextMarker = 4;
				prevMarker = 1;
			}
		}
		else if(nextMarker == 2) {
			if(m3Marker) {
				nextMarker = 3;
				prevMarker = 2;
			}
			else if(m4Marker) {
				nextMarker = 4;
				prevMarker = 2;
			}
			else {
				nextMarker = 1;
				prevMarker = 2;
			}
		}
		else if(nextMarker == 3) {
			if(m4Marker) {
				nextMarker = 4;
				prevMarker = 3;
			}
			else if(m1Marker) {
				nextMarker = 1;
				prevMarker = 3;
			}
			else {
				nextMarker = 2;
				prevMarker = 3;
			}
		}
		else {
			if(m1Marker) {
				nextMarker = 1;
				prevMarker = 4;
			}
			else if(m2Marker) {
				nextMarker = 2;
				prevMarker = 4;
			}
			else {
				nextMarker = 3;
				prevMarker = 4;
			}
		}
	}
	
	public boolean reachedDestination() {
		
		float dx, dy, dz = 0;
		if(nextMarker == 1) {
			dx = x1;
			dy = y1;
			dz = z1;
		}
		else if(nextMarker == 2) {
			dx = x2;
			dy = y2;
			dz = z2;
		}
		else if(nextMarker == 3) {
			dx = x3;
			dy = y3;
			dz = z3;
		}
		else {
			dx = x4;
			dy = y4;
			dz = z4;
		}
		
		double distance = Math.sqrt(Math.pow(x-dx, 2)+Math.pow(y-dy, 2)+Math.pow(z-dz, 2));
		return (distance <= speed);
	}
	
	public Vector3 getFlightVector() {
		
		Vector3 res;
		float dx, dy, dz = 0;
		if(nextMarker == 1) {
			dx = x1;
			dy = y1;
			dz = z1;
		}
		else if(nextMarker == 2) {
			dx = x2;
			dy = y2;
			dz = z2;
		}
		else if(nextMarker == 3) {
			dx = x3;
			dy = y3;
			dz = z3;
		}
		else {
			dx = x4;
			dy = y4;
			dz = z4;
		}
		
		return new Vector3(dx-x,dy-y,dz-z);
		
		
		
		/*
		Vector3 res;
		Vector3 v = new Vector3(x,y,z);
		if(nextMarker == 1) {
			res = a1.transform.getTranslation(v);
		}
		else if(nextMarker == 2) {
			res = a2.transform.getTranslation(v);
		}
		else if(nextMarker == 3) {
			res = a3.transform.getTranslation(v);
		}
		else {
			res = a4.transform.getTranslation(v);
		}		
		return res;	*/
	}
	
	/**
	 * 
	 * @param foundMarker
	 * @return {bestMatch, bestMatchPercentage, orientation}
	 */
	public int[] bestMarkerMatch(Mat foundMarker) {
		int bestMatch = -1;
		double bestMatchPercentage = -1;
		int orientation = 0;
		
		int[][] averagedFoundMarker = averagePixels(foundMarker);
		for (int i=0; i<NUMBER_OF_MARKERS; i++) {
			int[][] averagedMarker = averagedMarkers[i];
			double percentage[] = new double[4];
			
			for (int c=0; c<GRID_SIZE; c++) {
				for (int r=0; r<GRID_SIZE; r++) {
					percentage[0] += Math.abs(averagedFoundMarker[c][r]-averagedMarker[c][r]);
					percentage[1] += Math.abs(averagedFoundMarker[GRID_SIZE-1-r][c]-averagedMarker[c][r]);
					percentage[2] += Math.abs(averagedFoundMarker[GRID_SIZE-1-c][GRID_SIZE-1-r]-averagedMarker[c][r]);
					percentage[3] += Math.abs(averagedFoundMarker[r][GRID_SIZE-1-c]-averagedMarker[c][r]);
				}
			}
			for(int p=0; p<percentage.length; p++) {
				percentage[p] = ((255D - (percentage[p] / (double) (GRID_SIZE*GRID_SIZE))) / 255D ) * 100D;
				if (percentage[p] > bestMatchPercentage) {
					bestMatch = i;
					bestMatchPercentage = percentage[p];
					orientation = p*90;
				}
			}
		}
		
		return new int[] {bestMatch, (int) Math.round(bestMatchPercentage), orientation};
	}
	
	
	public int[][] averagePixels(Mat greyscaleImage) {
		int[][] grid20 = new int[GRID_SIZE][GRID_SIZE];
		int[][] grid20Count = new int[GRID_SIZE][GRID_SIZE];
		
		int cols = greyscaleImage.cols();
		int rows = greyscaleImage.rows();
		double pixelPrRow = ((double) rows) / (double) GRID_SIZE;
		double pixelPrCol = ((double) cols) / (double) GRID_SIZE;
		
		for(int c=0; c<cols; c++) {
			for(int r=0; r<rows; r++) {
				int value = (int) greyscaleImage.get(r, c)[0];
				int r20 = (int) (r/pixelPrRow);
				int c20 = (int) (c/pixelPrCol);
				
				grid20[c20][r20] = grid20[c20][r20] + value;
				grid20Count[c20][r20] = grid20Count[c20][r20] + 1;
			}
		}
		
		for(int c=0; c<GRID_SIZE; c++) {
			for(int r=0; r<GRID_SIZE; r++) {
				grid20[c][r] = grid20[c][r] / grid20Count[c][r];
			}
		}
		
		return grid20;
	}
	
	public Mat ourFindHomography(Point[] imagePoints, Point[] objPoints) {
		
		Mat h = new Mat(8,1,CvType.CV_64F);
		Mat p = new Mat(8,8,CvType.CV_64F);
		Mat res = new Mat(8,1,CvType.CV_64F);
		
		// F�rste point = (0,0)
		p.put(0, 0, -imagePoints[0].x); // -x1 -y1 -1 0 0 0 x1x1' y1x1'
		p.put(0, 1, -imagePoints[0].y); // 
		p.put(0, 2, -1);
		p.put(0, 3, 0);
		p.put(0, 4, 0);
		p.put(0, 5, 0);
		p.put(0, 6, imagePoints[0].x*objPoints[0].x);
		p.put(0, 7, imagePoints[0].y*objPoints[0].x);
		res.put(0, 0, -objPoints[0].x); // x1'
		
		p.put(1, 0, 0); // 0 0 0 -x1 -y1 -1 x1y1' y1y1'
		p.put(1, 1, 0); // 
		p.put(1, 2, 0);
		p.put(1, 3, -imagePoints[0].x);
		p.put(1, 4, -imagePoints[0].y);
		p.put(1, 5, -1);
		p.put(1, 6, imagePoints[0].x*objPoints[0].y);
		p.put(1, 7, imagePoints[0].y*objPoints[0].y);
		res.put(1, 0, -objPoints[0].y); // y1'
		
		// Andet point = (500,0)
		p.put(2, 0, -imagePoints[1].x); // -x1 -y1 -1 0 0 0 x1x1' y1x1'
		p.put(2, 1, -imagePoints[1].y); // 
		p.put(2, 2, -1);
		p.put(2, 3, 0);
		p.put(2, 4, 0);
		p.put(2, 5, 0);
		p.put(2, 6, imagePoints[1].x*objPoints[1].x);
		p.put(2, 7, imagePoints[1].y*objPoints[1].x);
		res.put(2, 0, -objPoints[1].x); // x1'
		
		p.put(3, 0, 0); // 0 0 0 -x1 -y1 -1 x1y1' y1y1'
		p.put(3, 1, 0); // 
		p.put(3, 2, 0);
		p.put(3, 3, -imagePoints[1].x);
		p.put(3, 4, -imagePoints[1].y);
		p.put(3, 5, -1);
		p.put(3, 6, imagePoints[1].x*objPoints[1].y);
		p.put(3, 7, imagePoints[1].y*objPoints[1].y);
		res.put(3, 0, -objPoints[1].y); // y1'
		
		// Tredje point = (500,500)
		p.put(4, 0, -imagePoints[2].x); // -x1 -y1 -1 0 0 0 x1x1' y1x1'
		p.put(4, 1, -imagePoints[2].y); // 
		p.put(4, 2, -1);
		p.put(4, 3, 0);
		p.put(4, 4, 0);
		p.put(4, 5, 0);
		p.put(4, 6, imagePoints[2].x*objPoints[2].x);
		p.put(4, 7, imagePoints[2].y*objPoints[2].x);
		res.put(4, 0, -objPoints[2].x); // x1'
		
		p.put(5, 0, 0); // 0 0 0 -x1 -y1 -1 x1y1' y1y1'
		p.put(5, 1, 0); // 
		p.put(5, 2, 0);
		p.put(5, 3, -imagePoints[2].x);
		p.put(5, 4, -imagePoints[2].y);
		p.put(5, 5, -1);
		p.put(5, 6, imagePoints[2].x*objPoints[2].y);
		p.put(5, 7, imagePoints[2].y*objPoints[2].y);
		res.put(5, 0, -objPoints[2].y); // y1'
		
		// Fjerde point = (0,500)
		p.put(6, 0, -imagePoints[3].x); // -x1 -y1 -1 0 0 0 x1x1' y1x1'
		p.put(6, 1, -imagePoints[3].y); // 
		p.put(6, 2, -1);
		p.put(6, 3, 0);
		p.put(6, 4, 0);
		p.put(6, 5, 0);
		p.put(6, 6, imagePoints[3].x*objPoints[3].x);
		p.put(6, 7, imagePoints[3].y*objPoints[3].x);
		res.put(6, 0, -objPoints[3].x); // x1'
		
		p.put(7, 0, 0); // 0 0 0 -x1 -y1 -1 x1y1' y1y1'
		p.put(7, 1, 0); // 
		p.put(7, 2, 0);
		p.put(7, 3, -imagePoints[3].x);
		p.put(7, 4, -imagePoints[3].y);
		p.put(7, 5, -1);
		p.put(7, 6, imagePoints[3].x*objPoints[3].y);
		p.put(7, 7, imagePoints[3].y*objPoints[3].y);
		res.put(7, 0, -objPoints[3].y); // y1'
		
		Core.solve(p, res, h, Core.DECOMP_SVD);
		// h er nu 8x1, skal laves om til 3x3
		
		Mat homography = new Mat(3,3,CvType.CV_64F);
		homography.put(0, 0, h.get(0,0));
		homography.put(0, 1, h.get(1,0));
		homography.put(0, 2, h.get(2,0));
		homography.put(1, 0, h.get(3,0));
		homography.put(1, 1, h.get(4,0));
		homography.put(1, 2, h.get(5,0));
		homography.put(2, 0, h.get(6,0));
		homography.put(2, 1, h.get(7,0));
		homography.put(2, 2, 1);
		
		return homography;
	}
}
