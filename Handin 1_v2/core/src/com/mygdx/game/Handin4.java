package com.mygdx.game;

import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
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
	
	public PerspectiveOffCenterCamera libGdxCam;
	Mat cameraMatrix;
	VideoCapture camera;
	int cameraResolutionX = 640;
	int cameraResolutionY = 480;
	
	public Environment environment;
    public ModelBatch modelBatch;
    public ModelInstance boxInstance;
    public Model boxModel;
    public Model xAxisModel;
    public Model yAxisModel;
    public Model zAxisModel;
    public Array<ModelInstance> instances = new Array<ModelInstance>();
    public float angle;
	
	@Override
	public void create() {
		libGdxCam = new PerspectiveOffCenterCamera();
		cameraMatrix = UtilAR.getDefaultIntrinsics(640f, 480f);
		libGdxCam.setByIntrinsics(cameraMatrix, 640f, 480f);
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
            new Material(ColorAttribute.createDiffuse(Color.RED)),
            Usage.Position | Usage.Normal);
        
        xAxisModel = modelBuilder.createArrow(new Vector3(0,0,0), new Vector3(10,0,0), 
				 new Material(ColorAttribute.createDiffuse(Color.RED)), 
				 Usage.Position | Usage.Normal);
		 
		yAxisModel = modelBuilder.createArrow(new Vector3(0,0,0), new Vector3(0,10,0), 
				 new Material(ColorAttribute.createDiffuse(Color.BLUE)), 
				 Usage.Position | Usage.Normal);
		 
		zAxisModel = modelBuilder.createArrow(new Vector3(0,0,0), new Vector3(0,0,-10), 
				 new Material(ColorAttribute.createDiffuse(Color.YELLOW)), 
				 Usage.Position | Usage.Normal);
        
	}
	
	@Override
	public void render() {
		
		angle = angle + 0.1f;
        if(angle > 359) angle = 0f;
		//angle++;
		//angle = angle % 360;
		
		float x = (float) Math.cos(angle);
		float y = 2.5f;
	    float z = (float) Math.sin(angle);
		
		Mat cameraImage = new Mat();
		camera.read(cameraImage);
		
		Mat greyImage = new Mat();
		Imgproc.cvtColor(cameraImage, greyImage, Imgproc.COLOR_BGR2GRAY);
		
		Mat binaryImage = new Mat(greyImage.size(), greyImage.type());
		Imgproc.threshold(greyImage, binaryImage, 100, 255, Imgproc.THRESH_BINARY);
		
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(binaryImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
		/*  image - Source, an 8-bit single-channel image. Non-zero pixels are treated as 1's. Zero pixels remain 0's, so the image is treated as binary. You can use "compare", "inRange", "threshold", "adaptiveThreshold", "Canny", and others to create a binary image out of a grayscale or color one. The function modifies the image while extracting the contours.
			contours - Detected contours. Each contour is stored as a vector of points.
			hierarchy - Optional output vector, containing information about the image topology. It has as many elements as the number of contours. For each i-th contour contours[i], the elements hierarchy[i][0], hiearchy[i][1], hiearchy[i][2], and hiearchy[i][3] are set to 0-based indices in contours of the next and previous contours at the same hierarchical level, the first child contour and the parent contour, respectively. If for the contour i there are no next, previous, parent, or nested contours, the corresponding elements of hierarchy[i] will be negative.
			mode - Contour retrieval mode.
			
		---	Brug IKKE RERT_EXTERNAL for extra points.
			CV_RETR_EXTERNAL retrieves only the extreme outer contours. It sets hierarchy[i][2]=hierarchy[i][3]=-1 for all the contours.
			CV_CHAIN_APPROX_SIMPLE compresses horizontal, vertical, and diagonal segments and leaves only their end points. For example, an up-right rectangular contour is encoded with 4 points.
		 */
		
		ArrayList<MatOfPoint> ap;
		ap = new ArrayList<MatOfPoint>();
		ArrayList<MatOfPoint> results = new ArrayList<MatOfPoint>();
		outerloop:
		for (int i=0; i<contours.size(); i++) {
			Point[] contour = contours.get(i).toArray();
			/*for (Point p : contour) {
				if (p.x <= 1 || p.y <=1 || p.x >= cameraResolutionX-2 || p.y >= cameraResolutionY-2) {
					continue outerloop;
				}
			}*/
			MatOfPoint2f curve = new MatOfPoint2f(contour);
			MatOfPoint2f approxCurve = new MatOfPoint2f();
			// Check om vi har en firkant. Vi filtrerede dog for kurver og lignende
			// oppe i findContours, så er dette skridt nødvendigt?
			// Er dog nødvendigt for extra points :)
			Imgproc.approxPolyDP(curve, approxCurve, Imgproc.arcLength(curve, true)*0.02, true);
			if (approxCurve.toList().size() == 4 && Imgproc.contourArea(approxCurve,true) > 2000) {
							
				ap.add(new MatOfPoint(approxCurve.toArray()));	
				results.add(new MatOfPoint(approxCurve.toArray()));
			}
		}
		
		Imgproc.drawContours(cameraImage, ap, 0, new Scalar(0,0,255));
		// Til at sortere i vores resultater kan vi se på krydsproduktet, men også
		// hierakiet. Vi kan også bruge approxCurve.total for at se på hvor lang edges er totalt,
		// men kan vel være det samme som at sortere fra mht. counterArea.
		
		Point3[] objPoints = new Point3[4];
		objPoints[0] = new Point3(-5,-5,0);
		objPoints[1] = new Point3(5,-5,0);
		objPoints[2] = new Point3(5,5,0);
		objPoints[3] = new Point3(-5,5,0);
		MatOfPoint3f objectPoints = new MatOfPoint3f(objPoints); // TODO: move to create()
		
		Point[] objPoints2 = new Point[4];
		objPoints2[0] = new Point(0,0);
		objPoints2[1] = new Point(500,0);
		objPoints2[2] = new Point(500,500);
		objPoints2[3] = new Point(0,500);
		MatOfPoint2f objectPoints2 = new MatOfPoint2f(objPoints2);
		
		if (!results.isEmpty()) {

			UtilAR.setNeutralCamera(libGdxCam);
			instances = new Array<ModelInstance>();
			
			//System.out.println(results.size());
			for(int i = 0; i < results.size(); i++) {
				MatOfPoint2f imagePoints = new MatOfPoint2f(results.get(i).toArray());
			
				Mat rvec = new Mat();
				Mat tvec = new Mat();
				Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix,
						UtilAR.getDefaultDistortionCoefficients(), rvec, tvec);
			
				ModelInstance xAxisInstance = new ModelInstance(xAxisModel);
				ModelInstance yAxisInstance = new ModelInstance(yAxisModel);
				ModelInstance zAxisInstance = new ModelInstance(zAxisModel);
				ModelInstance iBox = new ModelInstance(boxModel);
			        
			    Vector3 v = new Vector3(x*2.5f,0,-z*2.5f);
			    //iBox.transform.setToTranslation(v);
			    
				
				UtilAR.setTransformByRT(rvec, tvec, xAxisInstance.transform);
				UtilAR.setTransformByRT(rvec, tvec, yAxisInstance.transform);
				UtilAR.setTransformByRT(rvec, tvec, zAxisInstance.transform);
				UtilAR.setTransformByRT(rvec, tvec, iBox.transform);
				iBox.transform.translate(v);
				
				instances.add(xAxisInstance);
				instances.add(yAxisInstance);
				instances.add(zAxisInstance);
				instances.add(iBox);
				
				Mat homography = Calib3d.findHomography(imagePoints, objectPoints2);
		        Mat rectified = new Mat();
		        Imgproc.warpPerspective(cameraImage, rectified, homography, new Size(500,500));
		        
		        Mat greyHomo = new Mat();
				Imgproc.cvtColor(rectified, greyHomo, Imgproc.COLOR_BGR2GRAY);
				Mat binaryHomo = new Mat(greyHomo.size(), greyHomo.type());
				Imgproc.threshold(greyHomo, binaryHomo, 100, 255, Imgproc.THRESH_BINARY);
				
		        //averagePixels(binaryHomo);
		        //bestMarkerMatch(binaryHomo, new Mat[] {binaryHomo});
		        
		        UtilAR.imShow("key"+i,rectified);
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
	
	
	public int bestMarkerMatch(Mat foundMarker, Mat[] markers) {
		int bestMatch = -1;
		double bestMatchPercentage = -1;
		
		int[][] averagedFoundMarker = averagePixels(foundMarker);
		for (int i=0; i<markers.length; i++) {
			Mat marker = markers[i];
			int[][] averagedMarker = averagePixels(marker);
			double percentage = 0;
			
			for (int c=0; c<gridSize; c++) {
				for (int r=0; r<gridSize; r++) {
					percentage += Math.abs(averagedFoundMarker[c][r]-averagedMarker[c][r]);
				}
			}
			percentage = ((255D - (percentage / (double) (gridSize*gridSize))) / 255D ) * 100D;
			if (percentage > bestMatchPercentage) {
				bestMatch = i;
				bestMatchPercentage = percentage;
			}
		}
		
		return bestMatch;
	}
	
	private int gridSize = 20;
	
	public int[][] averagePixels(Mat greyscaleImage) {
		int[][] grid20 = new int[gridSize][gridSize];
		int[][] grid20Count = new int[gridSize][gridSize];
		
		int cols = greyscaleImage.cols();
		int rows = greyscaleImage.rows();
		double pixelPrRow = ((double) rows) / (double) gridSize;
		double pixelPrCol = ((double) cols) / (double) gridSize;
		
		for(int c=0; c<cols; c++) {
			for(int r=0; r<rows; r++) {
				int value = (int) greyscaleImage.get(r, c)[0];
				int r20 = (int) (r/pixelPrRow);
				int c20 = (int) (c/pixelPrCol);
				
				grid20[c20][r20] = grid20[c20][r20] + value;
				grid20Count[c20][r20] = grid20Count[c20][r20] + 1;
			}
		}
		
		for(int c=0; c<gridSize; c++) {
			for(int r=0; r<gridSize; r++) {
				grid20[c][r] = grid20[c][r] / grid20Count[c][r];
			}
		}
		
		return grid20;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
