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
    private float speed = 15f;
    private float sprintMultiplier = 3f;
    private boolean isSprinting = false;
    private float worldWidth = 1600f;
    private float worldHeight = 1600f;
    private float colWidth = 14f;
    private float colHeight = 14f;

    public interface CollisionChecker {
        boolean isBlocked(int tileX, int tileY);
    }
    private CollisionChecker collisionChecker;

    public interface TileSpeedModifier {
        float getModifier(int tileX, int tileY);
    }
    private TileSpeedModifier tileSpeedModifier;

    private final String characterFolder;

    public void setCollisionChecker(CollisionChecker checker) {
        this.collisionChecker = checker;
    }

    public void setTileSpeedModifier(TileSpeedModifier modifier) {
        this.tileSpeedModifier = modifier;
    }

    public void setWorldBounds(float width, float height) {
        this.worldWidth = width;
        this.worldHeight = height;
    }

    public boolean isSprinting() {
        return isSprinting;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

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

        isSprinting = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

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

        x = MathUtils.clamp(x, 0, worldWidth - currentFrame.getRegionWidth());
        y = MathUtils.clamp(y, 0, worldHeight - currentFrame.getRegionHeight());
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

        // Compute speed: base → sprint → tile modifier
        float currentSpeed = isSprinting ? speed * sprintMultiplier : speed;
        if (tileSpeedModifier != null) {
            int tx = (int)Math.floor((x + colOffsetX() + colWidth / 2f) / 32);
            int ty = (int)Math.floor((y + colOffsetY() + colHeight / 2f) / 32);
            currentSpeed *= tileSpeedModifier.getModifier(tx, ty);
        }
        float newX = x + dx * currentSpeed * delta;
        float newY = y + dy * currentSpeed * delta;

        // Use centered hitbox for collision
        float cx = x + colOffsetX();
        float cy = y + colOffsetY();
        float newCx = newX + colOffsetX();
        float newCy = newY + colOffsetY();

        if (collisionChecker != null) {
            if (!isBlockedAt(newCx, cy)) x = newX;
            if (!isBlockedAt(cx, newCy)) y = newY;
        } else {
            x = newX;
            y = newY;
        }

        return true;
    }

    private float colOffsetX() {
        return (currentFrame.getRegionWidth() - colWidth) / 2f;
    }
    private float colOffsetY() {
        return 0f;
    }

    private boolean isBlockedAt(float px, float py) {
        if (collisionChecker == null) return false;
        int startTx = (int)Math.floor(px / 32);
        int endTx   = (int)Math.floor((px + colWidth - 1) / 32);
        int startTy = (int)Math.floor(py / 32);
        int endTy   = (int)Math.floor((py + colHeight - 1) / 32);
        for (int tx = startTx; tx <= endTx; tx++) {
            for (int ty = startTy; ty <= endTy; ty++) {
                if (collisionChecker.isBlocked(tx, ty)) return true;
            }
        }
        return false;
    }

    public void draw(SpriteBatch batch) {
        batch.draw(currentFrame, x, y);
    }

    public void dispose() {
        for (Texture t : allTextures) t.dispose();
    }

    // Player.java ထဲက class အောက်ခြေနားမှာ ဒီကုဒ်လေး ထည့်ပေးပါ
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    // Player ရဲ့ လက်ရှိပုံ အကျယ်နဲ့ အမြင့်ကို ယူဖို့ (Camera ကို အလယ်တည့်တည့် ညှိချင်ရင် သုံးရအောင်ပါ)
    public float getWidth() {
        return currentFrame != null ? currentFrame.getRegionWidth() : 0;
    }

    public float getHeight() {
        return currentFrame != null ? currentFrame.getRegionHeight() : 0;
    }
}
