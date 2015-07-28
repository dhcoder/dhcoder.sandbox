package dhcoder.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import dhcoder.support.annotations.NotNull;

/**
 * Convenience shader class that wraps a vertex and fragment shader.
 */
public final class BasicShader {

    @NotNull private ShaderProgram myProgram;

    /**
     * Shader file names should be "vertex.glsl" and "fragment.glsl" and share a common path.
     */
    public BasicShader(@NotNull String path) {
        String vertexShader = Gdx.files.internal(String.format("%s/vertex.glsl", path)).readString();
        String fragmentShader = Gdx.files.internal(String.format("%s/fragment.glsl", path)).readString();
        myProgram = new ShaderProgram(vertexShader, fragmentShader);
    }

    @NotNull
    public ShaderProgram getProgram() {
        return myProgram;
    }
}
