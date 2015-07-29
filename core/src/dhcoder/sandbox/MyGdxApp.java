package dhcoder.sandbox;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class MyGdxApp extends ApplicationAdapter {

    public static final String TAG = "SANDBOX";
    private Camera myCamera;
    private ShapeRenderer myShapeRenderer;
    private SpriteBatch mySpriteBatch;
    private Texture myLogo;

    @Override
    public void create() {
        myCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        myShapeRenderer = new ShapeRenderer();
        myShapeRenderer.setProjectionMatrix(myCamera.combined);
        mySpriteBatch = new SpriteBatch();
        mySpriteBatch.setProjectionMatrix(myCamera.combined);
        myLogo = new Texture(Gdx.files.internal("badlogic.jpg"));
        mySpriteBatch.setShader(new BasicShader("shaders/inverse").getProgram());

        Gdx.input.setInputProcessor(new MyInputHandler());
    }

    @Override
    public void render() {
        update(Gdx.graphics.getRawDeltaTime());

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        myShapeRenderer.begin(ShapeType.Filled);
        myShapeRenderer.setColor(Color.DARK_GRAY);
        myShapeRenderer.circle(0f, 0f, myLogo.getWidth() * 2f / 3f);
        myShapeRenderer.end();

        mySpriteBatch.begin();
        mySpriteBatch.draw(myLogo, -myLogo.getHeight() / 2f, -myLogo.getWidth() / 2f);
        mySpriteBatch.end();
    }

    private void update(float deltaTime) {
        // YOUR CODE HERE
    }

    private class MyInputHandler extends InputAdapter {
        private Vector3 myTouch3d = new Vector3();
        private Vector2 myTouch = new Vector2();

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            return true;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            updateMyTouch(screenX, screenY);
            return true;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            updateMyTouch(screenX, screenY);
            return true;
        }

        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
                Gdx.app.log(TAG, "Quitting");
                Gdx.app.exit();
                return true;
            }

            return false;
        }

        // Call this and then myTouch vec will have screen coordinates
        private void updateMyTouch(int screenX, int screenY) {
            myTouch3d.set(screenX, screenY, 0f);
            myCamera.unproject(myTouch3d);
            myTouch.set(myTouch3d.x, myTouch3d.y);
        }
    }
}
