varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform mat4 u_projTrans;

void main() {
        vec3 color = texture2D(u_texture, v_texCoords).rgb;
        vec3 inverse = vec3(1.0) - color;
        gl_FragColor = vec4(inverse, 1.0);
}