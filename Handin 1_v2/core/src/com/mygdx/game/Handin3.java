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

public class Handin3 extends ApplicationAdapter {
	
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
    public ModelInstance xAxisInstance;
    public ModelInstance yAxisInstance;
    public ModelInstance zAxisInstance;
	
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
        
        boxInstance = new ModelInstance(boxModel); 
		//boxInstance.transform.translate(0.5F, 0.5F, 0.5F);
        
        xAxisModel = modelBuilder.createArrow(new Vector3(0,0,0), new Vector3(10,0,0), 
				 new Material(ColorAttribute.createDiffuse(Color.RED)), 
				 Usage.Position | Usage.Normal);
		 
		yAxisModel = modelBuilder.createArrow(new Vector3(0,0,0), new Vector3(0,10,0), 
				 new Material(ColorAttribute.createDiffuse(Color.BLUE)), 
				 Usage.Position | Usage.Normal);
		 
		zAxisModel = modelBuilder.createArrow(new Vector3(0,0,0), new Vector3(0,0,10), 
				 new Material(ColorAttribute.createDiffuse(Color.YELLOW)), 
				 Usage.Position | Usage.Normal);
        

        xAxisInstance = new ModelInstance(xAxisModel);
        yAxisInstance = new ModelInstance(yAxisModel);
        zAxisInstance = new ModelInstance(zAxisModel);
	}
	
	@Override
	public void render() {
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
		ArrayList<MatOfPoint> results = new ArrayList<MatOfPoint>();
		outerloop:
		for (int i=0; i<contours.size(); i++) {
			Point[] contour = contours.get(i).toArray();
			for (Point p : contour) {
				if (p.x <= 1 || p.y <=1 || p.x >= cameraResolutionX-2 || p.y >= cameraResolutionY-2) {
					continue outerloop;
				}
			}
			MatOfPoint2f curve = new MatOfPoint2f(contour);
			MatOfPoint2f approxCurve = new MatOfPoint2f();
			// Check om vi har en firkant. Vi filtrerede dog for kurver og lignende
			// oppe i findContours, så er dette skridt nødvendigt?
			// Er dog nødvendigt for extra points :)
			Imgproc.approxPolyDP(curve, approxCurve, Imgproc.arcLength(curve, true)*0.02, true);
			if (approxCurve.toList().size() == 4 && Imgproc.contourArea(approxCurve) > 2000) {
				
				 
				ap = new ArrayList<MatOfPoint>();
				ap.add(new MatOfPoint(approxCurve.toArray()));
				Imgproc.drawContours(cameraImage, ap, 0, new Scalar(0,0,255));
				results.add(new MatOfPoint(approxCurve.toArray()));
			}
		}
		
		// Til at sortere i vores resultater kan vi se på krydsproduktet, men også
		// hierakiet. Vi kan også bruge approxCurve.total for at se på hvor lang edges er totalt,
		// men kan vel være det samme som at sortere fra mht. counterArea.
		
		Point3[] objPoints = new Point3[4];
		objPoints[0] = new Point3(-5,-5,0);
		objPoints[1] = new Point3(5,-5,0);
		objPoints[2] = new Point3(5,5,0);
		objPoints[3] = new Point3(-5,5,0);
		MatOfPoint3f objectPoints = new MatOfPoint3f(objPoints); // TODO: move to create()
		
		if (!results.isEmpty()) {
			// --- Kun første resultat, fix for extra.
			MatOfPoint2f imagePoints = new MatOfPoint2f(results.get(0).toArray());
			//System.out.println(imagePoints.toList());
			
			Mat rvec = new Mat();
			Mat tvec = new Mat();
			Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix,
					UtilAR.getDefaultDistortionCoefficients(), rvec, tvec);
			
			// For extra skal vi ikke bruge denne.
			// Brug et neutralt kamera.
			// Util.setTransformRT for hver enkelt marker.
			UtilAR.setCameraByRT(rvec, tvec, libGdxCam);
			
			// Vi skal ikke tegne en box på marker,
			// vi skal vise et rectified billede af det der er
			// inde for markeren, og i extra point skal vi lave
			// et koordinatsystem med en box der cirkler omkring.
			UtilAR.imDrawBackground(cameraImage);
			modelBatch.begin(libGdxCam);
			//modelBatch.render(boxInstance, environment);
			modelBatch.render(xAxisInstance,environment);
			modelBatch.render(yAxisInstance,environment);
			modelBatch.render(zAxisInstance,environment);
	        modelBatch.end();
	        
	        
	        Point[] objPoints2 = new Point[4];
			objPoints2[0] = new Point(-5,-5);
			objPoints2[1] = new Point(5,-5);
			objPoints2[2] = new Point(5,5);
			objPoints2[3] = new Point(-5,5);
			MatOfPoint2f objectPoints2 = new MatOfPoint2f(objPoints2); // TODO: move to create()
			
	        Mat homography = Calib3d.findHomography(imagePoints, objectPoints2, Calib3d.RANSAC, 10);
	        //Mat rectified = cameraImage.mul(homography);
	        Mat rectified = new Mat();
	        Imgproc.warpPerspective(cameraImage, rectified, homography, new Size(cameraImage.cols(),cameraImage.rows()));
	        UtilAR.imShow("key",rectified);
	        
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
}
