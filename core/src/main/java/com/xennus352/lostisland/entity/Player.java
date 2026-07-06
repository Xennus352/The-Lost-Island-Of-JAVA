package com.xennus352.lostisland.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import java.util.HashMap;
import java.util.Map;

public class Player {
    private Map<String, Texture> textures;
    private Map<String, Animation<TextureRegion>> attackAnimations;
    private Array<Texture> attackTextures; // For easy disposal

    private TextureRegion currentFrame;
    private float stateTime = 0f;
    private boolean isAttacking = false;
    private String lastDirection = "S";

    private float x, y;
    private float speed = 200f;
    private static final float VIRTUAL_WIDTH = 640;
    private static final float VIRTUAL_HEIGHT = 480;

    public Player(float startX, float startY) {
        this.x = startX;
        this.y = startY;

        // Load walking textures
        textures = new HashMap<>();
        textures.put("N", new Texture("Fighter/walk/north.png"));
        textures.put("S", new Texture("Fighter/walk/south.png"));
        textures.put("E", new Texture("Fighter/walk/east.png"));
        textures.put("W", new Texture("Fighter/walk/west.png"));
        textures.put("NE", new Texture("Fighter/walk/north-east.png"));
        textures.put("NW", new Texture("Fighter/walk/north-west.png"));
        textures.put("SE", new Texture("Fighter/walk/south-east.png"));
        textures.put("SW", new Texture("Fighter/walk/south-west.png"));

        // Load attack animations
        attackTextures = new Array<>();
        attackAnimations = new HashMap<>();
        attackAnimations.put("N", createAttackAnimation("north"));
        attackAnimations.put("S", createAttackAnimation("south"));
        attackAnimations.put("E", createAttackAnimation("east"));
        attackAnimations.put("W", createAttackAnimation("west"));

        currentFrame = new TextureRegion(textures.get("S"));
    }

    private Animation<TextureRegion> createAttackAnimation(String direction) {
        TextureRegion[] frames = new TextureRegion[10];
        for (int i = 0; i < 10; i++) {
            Texture texture = new Texture("Fighter/attack/" + direction +"/"+direction+ "_attack_frame_" + i + ".png");
            attackTextures.add(texture);
            frames[i] = new TextureRegion(texture);
        }
        return new Animation<>(0.06f, frames);
    }

    public void update(float delta) {
        stateTime += delta;
        boolean up = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean down = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        if (Gdx.input.isKeyJustPressed(Input.Keys.J) && !isAttacking) {
            isAttacking = true;
            stateTime = 0f;
        }

        if (isAttacking) {
            String attackDir = (lastDirection.contains("N")) ? "N" : (lastDirection.contains("S")) ? "S" : (lastDirection.contains("E")) ? "E" : "W";
            currentFrame = attackAnimations.get(attackDir).getKeyFrame(stateTime, false);
            if (attackAnimations.get(attackDir).isAnimationFinished(stateTime)) isAttacking = false;
        } else {
            if (up) y += speed * delta;
            if (down) y -= speed * delta;
            if (left) x -= speed * delta;
            if (right) x += speed * delta;

            String dir = "";
            if (up) dir += "N"; if (down) dir += "S"; if (left) dir += "W"; if (right) dir += "E";
            if (!dir.isEmpty()) {
                lastDirection = dir;
                currentFrame = new TextureRegion(textures.get(dir));
            }
        }

        x = MathUtils.clamp(x, 0, VIRTUAL_WIDTH - currentFrame.getRegionWidth());
        y = MathUtils.clamp(y, 0, VIRTUAL_HEIGHT - currentFrame.getRegionHeight());
    }

    public void draw(SpriteBatch batch) {
        batch.draw(currentFrame, x, y);
    }

    public void dispose() {
        for (Texture t : textures.values()) t.dispose();
        for (Texture t : attackTextures) t.dispose();
    }
}
