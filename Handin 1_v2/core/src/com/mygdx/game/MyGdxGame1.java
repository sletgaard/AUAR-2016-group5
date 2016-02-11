package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

public class MyGdxGame1 extends ApplicationAdapter {
	public Environment environment;
	public PerspectiveCamera cam;
	public CameraInputController camController;
    public ModelBatch modelBatch;
    public ModelInstance box1Instance;
    public ModelInstance box2Instance;
    public ModelInstance box3Instance;
    public ModelInstance xAxisInstance;
    public ModelInstance yAxisInstance;
    public ModelInstance zAxisInstance;
    public Model box1Model;
    public Model box2Model;
    public Model box3Model;
    public Model xAxisModel;
    public Model yAxisModel;
    public Model zAxisModel;
    
    public int angle;
    public int focusedBox;
    
	
	SpriteBatch batch;
	Texture img;
	
	@Override
	public void create () {
		environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
        
		modelBatch = new ModelBatch();
		
		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(5f, 5f, 5f);
        cam.lookAt(0,0,0);
        cam.near = 1f;
        cam.far = 300f;
        cam.update();
        
        ModelBuilder modelBuilder = new ModelBuilder();
        box1Model = modelBuilder.createBox(1f, 1f, 1f, 
            new Material(ColorAttribute.createDiffuse(Color.WHITE)),
            Usage.Position | Usage.Normal);
        
        box2Model = modelBuilder.createBox(0.5f, 0.5f, 0.5f, 
        		new Material(ColorAttribute.createDiffuse(Color.WHITE)),
        		Usage.Position | Usage.Normal);
        
        box3Model = modelBuilder.createBox(0.25f, 0.25f, 0.25f, 
        		new Material(ColorAttribute.createDiffuse(Color.WHITE)),
        		Usage.Position | Usage.Normal);
        
        xAxisModel = modelBuilder.createArrow(0F, 0F, 0F, 5F, 0F, 0F,
        		0.02F, 0.1F, 50, 1,
        		new Material(ColorAttribute.createDiffuse(Color.BLUE)), Usage.Position | Usage.Normal);
        
        yAxisModel = modelBuilder.createArrow(0F, 0F, 0F, 0F, 5F, 0F,
        		0.02F, 0.1F, 50, 1,
        		new Material(ColorAttribute.createDiffuse(Color.GREEN)), Usage.Position | Usage.Normal);
        
        zAxisModel = modelBuilder.createArrow(0F, 0F, 0F, 0F, 0F, 5F,
        		0.02F, 0.1F, 50, 1,
        		new Material(ColorAttribute.createDiffuse(Color.RED)), Usage.Position | Usage.Normal);
        
        box1Instance = new ModelInstance(box1Model);
        box2Instance = new ModelInstance(box2Model);
        box3Instance = new ModelInstance(box3Model);
        xAxisInstance = new ModelInstance(xAxisModel);
        yAxisInstance = new ModelInstance(yAxisModel);
        zAxisInstance = new ModelInstance(zAxisModel);
        
        box2Instance.transform.translate(2.5F, 0, 0);
        
        Vector3 v3 = box2Instance.transform.getTranslation(new Vector3(0, 0, 0));
        box3Instance.transform.translate(v3);
        box3Instance.transform.translate(1F,0,0);
        
        
        InputMultiplexer multiplexer = new InputMultiplexer();
        camController = new CameraInputController(cam);
        multiplexer.addProcessor(camController);
        multiplexer.addProcessor(new eventListener());
        //Gdx.input.setInputProcessor(camController);
        //Gdx.input.setInputProcessor(new eventListener());
        Gdx.input.setInputProcessor(multiplexer);
        
	}

	@Override
	public void render () {
		angle++;
		angle = angle % 360;
		camController.update();
		
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        
        box1Instance.transform.rotate(0, 1, 0, 0.5F);
        
        box2Instance.transform.translate(-2.5F, 0F, 0F);
        box2Instance.transform.rotate(0, 1, 0, 1);
        box2Instance.transform.translate(2.5F, 0F, 0F);
        
        Vector3 box2Translation = box2Instance.transform.getTranslation(new Vector3(0, 0, 0));
        Vector3 box3Translation = box3Instance.transform.getTranslation(new Vector3(0, 0, 0));
        
        box3Instance.transform.translate(-box3Translation.x,-box3Translation.y,-box3Translation.z);
        box3Instance.transform.translate(box2Translation);
        box3Instance.transform.rotate(0, 1, 0, 2*angle);
        box3Instance.transform.translate(1F,0,0);
        box3Instance.transform.rotate(0, 1, 0, -(2*angle));
        

        modelBatch.begin(cam);
        modelBatch.render(box1Instance, environment);
        modelBatch.render(box2Instance, environment);
        modelBatch.render(box3Instance, environment);
        modelBatch.render(xAxisInstance, environment);
        modelBatch.render(yAxisInstance, environment);
        modelBatch.render(zAxisInstance, environment);
        modelBatch.end();
	}

    @Override
    public void dispose () {
        modelBatch.dispose();
        box1Model.dispose();
        box2Model.dispose();
        box3Model.dispose();
        xAxisModel.dispose();
        yAxisModel.dispose();
        zAxisModel.dispose();
    }
    
    public void resize(int width, int height) {
    	cam.viewportWidth=Gdx.graphics.getWidth();
    	cam.viewportHeight=Gdx.graphics.getHeight();
    	cam.update();
    }
    
    private class eventListener implements InputProcessor {

		@Override
		public boolean keyDown(int keycode) {
			int lastFocus = focusedBox;
			if (keycode == Input.Keys.RIGHT) {
				focusedBox++;
				if (focusedBox > 3) {
					focusedBox = 1;
				}
			} else if (keycode == Input.Keys.LEFT) {
				focusedBox--;
				if (focusedBox < 1) {
					focusedBox = 3;
				}
			}
			if (focusedBox != lastFocus) {
				Vector3 translation = null;
				if (focusedBox == 1) {
					translation = box1Instance.transform.getTranslation(new Vector3(0,0,0));
				} else if (focusedBox == 2) {
					translation = box2Instance.transform.getTranslation(new Vector3(0,0,0));
				} else {
					translation = box3Instance.transform.getTranslation(new Vector3(0,0,0));
				}
				cam.lookAt(translation);				
				cam.update();
				
			}
			return false;
		}

		@Override
		public boolean keyUp(int keycode) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean keyTyped(char character) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean mouseMoved(int screenX, int screenY) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean scrolled(int amount) {
			// TODO Auto-generated method stub
			return false;
		}
    	
    }
}
