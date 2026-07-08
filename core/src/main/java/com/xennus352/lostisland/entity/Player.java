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
    private Map<String, Animation<TextureRegion>> walkAnimations;
    private Map<String, Animation<TextureRegion>> attackAnimations;
    private Array<Texture> allTextures; // Store ALL textures here for easy disposal

    private TextureRegion currentFrame;
    private float stateTime = 0f;
    private boolean isAttacking = false;
    private String lastDirection = "S";

    private Map<String, TextureRegion> idleTextures; // NEW
    private boolean isMoving = false; // NEW

    private float x, y;
    private float speed = 10f;
    private static final float VIRTUAL_WIDTH = 640;
    private static final float VIRTUAL_HEIGHT = 480;

    public Player(float startX, float startY) {
        this.x = startX;
        this.y = startY;

        // Initialize the array before any method uses it
        this.allTextures = new Array<>();

        // Inside Player constructor...
        idleTextures = new HashMap<>();
        idleTextures.put("N", new TextureRegion(new Texture("Fighter/north.png")));
        idleTextures.put("S", new TextureRegion(new Texture("Fighter/south.png")));
        idleTextures.put("E", new TextureRegion(new Texture("Fighter/east.png")));
        idleTextures.put("W", new TextureRegion(new Texture("Fighter/west.png")));
        // Add these to allTextures so they are disposed correctly!
        allTextures.add(idleTextures.get("N").getTexture());
        allTextures.add(idleTextures.get("S").getTexture());
        allTextures.add(idleTextures.get("E").getTexture());
        allTextures.add(idleTextures.get("W").getTexture());

        walkAnimations = new HashMap<>();
        walkAnimations.put("N", createAnimation("Fighter/walk/north/", "north_walk_", 9));
        walkAnimations.put("S", createAnimation("Fighter/walk/south/", "south_walk_", 9));
        walkAnimations.put("E", createAnimation("Fighter/walk/east/", "east_walk_", 9));
        walkAnimations.put("W", createAnimation("Fighter/walk/west/", "west_walk_", 9));

        attackAnimations = new HashMap<>();
        attackAnimations.put("N", createAnimation("Fighter/attack/north/", "north_attack_frame_", 10));
        attackAnimations.put("S", createAnimation("Fighter/attack/south/", "south_attack_frame_", 10));
        attackAnimations.put("E", createAnimation("Fighter/attack/east/", "east_attack_frame_", 10));
        attackAnimations.put("W", createAnimation("Fighter/attack/west/", "west_attack_frame_", 10));

        currentFrame = walkAnimations.get("S").getKeyFrame(0);
    }

    private Animation<TextureRegion> createAnimation(String path, String prefix, int count) {
        TextureRegion[] frames = new TextureRegion[count];
        for (int i = 0; i < count; i++) {
            Texture texture = new Texture(path + prefix + i + ".png");
            allTextures.add(texture);
            frames[i] = new TextureRegion(texture);
        }
        return new Animation<>(0.06f, frames);
    }

    public void update(float delta) {
        stateTime += delta;

        if (Gdx.input.isKeyJustPressed(Input.Keys.J) && !isAttacking) {
            isAttacking = true;
            stateTime = 0f;
        }

        if (isAttacking) {
            String dir = getDirectionKey();
            Animation<TextureRegion> anim = attackAnimations.get(dir);
            if (anim != null) {
                currentFrame = anim.getKeyFrame(stateTime, false);
                if (anim.isAnimationFinished(stateTime)) isAttacking = false;
            }
        } else {
            isMoving = handleMovement(delta);
            String dir = getDirectionKey();
            Animation<TextureRegion> anim = walkAnimations.get(dir);
            if (anim != null) {
                currentFrame = isMoving ? anim.getKeyFrame(stateTime, true) : idleTextures.get(dir);
            }
        }

        x = MathUtils.clamp(x, 0, VIRTUAL_WIDTH - currentFrame.getRegionWidth());
        y = MathUtils.clamp(y, 0, VIRTUAL_HEIGHT - currentFrame.getRegionHeight());
    }

    private String getDirectionKey() {
        // 1. If N or S is present, prioritize vertical
        if (lastDirection.contains("N")) return "N";
        if (lastDirection.contains("S")) return "S";

        // 2. Otherwise, check for East or West
        if (lastDirection.contains("E")) return "E";
        if (lastDirection.contains("W")) return "W";

        // 3. Default fallback
        return "S";
    }
    private boolean handleMovement(float delta) {
        boolean up = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean down = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        boolean moving = up || down || left || right;

        if (moving) {
            if (up) y += speed * delta;
            if (down) y -= speed * delta;
            if (left) x -= speed * delta;
            if (right) x += speed * delta;

            String dir = "";
            if (up) dir += "N";
            if (down) dir += "S";
            if (left) dir += "W";
            if (right) dir += "E";

            if (!dir.isEmpty()) lastDirection = dir;
        }
        return moving; // Return true if moving
    }

    public void draw(SpriteBatch batch) {
        batch.draw(currentFrame, x, y);
    }

    public void dispose() {
        for (Texture t : allTextures) t.dispose();
    }
}
