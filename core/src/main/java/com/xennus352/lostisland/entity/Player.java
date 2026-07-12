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
    private Array<Texture> allTextures;
    private Map<String, TextureRegion> idleTextures;

    private TextureRegion currentFrame;
    private float stateTime = 0f;
    private boolean isAttacking = false;
    private String lastDirection = "S";
    private boolean isMoving = false;

    private float x, y;
    private float speed = 10f;
    private static final float VIRTUAL_WIDTH = 640;
    private static final float VIRTUAL_HEIGHT = 480;

    private final String characterFolder;

    public Player(float startX, float startY, String characterFolder) {
        this.x = startX;
        this.y = startY;
        this.characterFolder = characterFolder;
        this.allTextures = new Array<>();

        idleTextures = new HashMap<>();
        idleTextures.put("N", new TextureRegion(new Texture(characterFolder + "/north.png")));
        idleTextures.put("S", new TextureRegion(new Texture(characterFolder + "/south.png")));
        idleTextures.put("E", new TextureRegion(new Texture(characterFolder + "/east.png")));
        idleTextures.put("W", new TextureRegion(new Texture(characterFolder + "/west.png")));
        idleTextures.put("NE", new TextureRegion(new Texture(characterFolder + "/north-east.png")));
        idleTextures.put("SE", new TextureRegion(new Texture(characterFolder + "/south-east.png")));
        idleTextures.put("NW", new TextureRegion(new Texture(characterFolder + "/north-west.png")));
        idleTextures.put("SW", new TextureRegion(new Texture(characterFolder + "/south-west.png")));

        allTextures.add(idleTextures.get("N").getTexture());
        allTextures.add(idleTextures.get("S").getTexture());
        allTextures.add(idleTextures.get("E").getTexture());
        allTextures.add(idleTextures.get("W").getTexture());
        allTextures.add(idleTextures.get("NE").getTexture());
        allTextures.add(idleTextures.get("SE").getTexture());
        allTextures.add(idleTextures.get("NW").getTexture());
        allTextures.add(idleTextures.get("SW").getTexture());

        walkAnimations = new HashMap<>();
        walkAnimations.put("N", createAnimation(characterFolder + "/walk/north/", "north_walk_", 9));
        walkAnimations.put("S", createAnimation(characterFolder + "/walk/south/", "south_walk_", 9));
        walkAnimations.put("E", createAnimation(characterFolder + "/walk/east/", "east_walk_", 9));
        walkAnimations.put("W", createAnimation(characterFolder + "/walk/west/", "west_walk_", 9));
        walkAnimations.put("NE", createAnimation(characterFolder + "/walk/north-east/", "north_east_walk_", 8));
        walkAnimations.put("SE", createAnimation(characterFolder + "/walk/south-east/", "south_east_walk_", 8));
        walkAnimations.put("NW", createAnimation(characterFolder + "/walk/north-west/", "north_west_walk_", 8));
        walkAnimations.put("SW", createAnimation(characterFolder + "/walk/south-west/", "south_west_walk_", 8));


        attackAnimations = new HashMap<>();
        attackAnimations.put("N", createAnimation(characterFolder + "/attack/north/", "north_attack_", 10));
        attackAnimations.put("S", createAnimation(characterFolder + "/attack/south/", "south_attack_", 10));
        attackAnimations.put("E", createAnimation(characterFolder + "/attack/east/", "east_attack_", 10));
        attackAnimations.put("W", createAnimation(characterFolder + "/attack/west/", "west_attack_", 10));
        attackAnimations.put("NE", createAnimation(characterFolder + "/attack/north-east/", "north_east_attack_", 9));
        attackAnimations.put("SE", createAnimation(characterFolder + "/attack/south-east/", "south_east_attack_", 9));
        attackAnimations.put("NW", createAnimation(characterFolder + "/attack/north-west/", "north_west_attack_", 9));
        attackAnimations.put("SW", createAnimation(characterFolder + "/attack/south-west/", "south_west_attack_", 9));

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
        return lastDirection;
    }

    private boolean handleMovement(float delta) {

        boolean up    = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean down  = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean left  = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        float dx = 0;
        float dy = 0;

        if (up) dy += 1;
        if (down) dy -= 1;
        if (left) dx -= 1;
        if (right) dx += 1;

        if (dx == 0 && dy == 0)
            return false;

        // Normalize diagonal movement
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        dx /= len;
        dy /= len;

        x += dx * speed * delta;
        y += dy * speed * delta;

        // Determine direction
        if (up && right)
            lastDirection = "NE";
        else if (up && left)
            lastDirection = "NW";
        else if (down && right)
            lastDirection = "SE";
        else if (down && left)
            lastDirection = "SW";
        else if (up)
            lastDirection = "N";
        else if (down)
            lastDirection = "S";
        else if (left)
            lastDirection = "W";
        else if (right)
            lastDirection = "E";

        return true;
    }

    public void draw(SpriteBatch batch) {
        batch.draw(currentFrame, x, y);
    }

    public void dispose() {
        for (Texture t : allTextures) t.dispose();
    }
}
