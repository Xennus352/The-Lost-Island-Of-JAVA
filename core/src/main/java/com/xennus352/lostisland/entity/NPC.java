package com.xennus352.lostisland.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import java.util.HashMap;
import java.util.Map;

public class NPC {
    public enum State { WANDER, ALERT, CHASE, ATTACK, FLEE, SEARCH, DYING }

    private State state = State.WANDER;
    private State prevState = State.WANDER;

    private float x, y;
    private float spawnX, spawnY;
    private float wanderRadius = 120f;
    private float speed = 12f;
    private String lastDirection = "S";

    private int health = 50;
    private int maxHealth = 50;

    private float attackCooldown = 1.0f;
    private float attackTimer = 0f;
    private int attackDamage = 10;
    private float attackRange = 28f;
    private float aggroRange = 150f;
    private float deaggroRange = 300f;
    private float fleeThreshold = 0.3f;

    private Map<String, TextureRegion> idleTextures;
    private Map<String, TextureRegion> chaseTextures;
    private Map<String, TextureRegion> attackTextures;
    private Map<String, TextureRegion> dieTextures;
    private TextureRegion currentFrame;
    private float spriteScale = 0.6f;
    private Array<Texture> allTextures;

    private Player player;
    private Player.CollisionChecker collisionChecker;

    private String npcName;
    private float colWidth = 14f;
    private float colHeight = 14f;
    private float worldWidth = 1600f;
    private float worldHeight = 1600f;

    private float stateTime = 0f;

    private float wanderTargetX, wanderTargetY;
    private float wanderPauseTimer = 0f;
    private boolean isWanderMoving = false;

    private float lastKnownPlayerX, lastKnownPlayerY;
    private float alertTimer = 0f;
    private float maxAlertTime = 3f;

    private float searchTimer = 0f;
    private float maxSearchTime = 5f;
    private boolean searchReached = false;

    private float deathTimer = 0f;
    private float deathDuration = 1.5f;
    private boolean fullyDead = false;

    private String[] dialogueLines = {"Hello there.", "Stay safe out here."};

    public void setDialogue(String[] lines) { this.dialogueLines = lines; }
    public String[] getDialogue() { return dialogueLines; }
    public TextureRegion getCurrentFrame() { return currentFrame; }

    private static final String[] DIRS = {"N", "S", "E", "W", "NE", "SE", "NW", "SW"};
    private static final String[] FILE_NAMES = {"north", "south", "east", "west", "north-east", "south-east", "north-west", "south-west"};
    private static final String[] IDLE_DIRS = {"S", "SE", "E", "NE", "N", "NW", "W", "SW"};

    public NPC(float startX, float startY, String npcFolder, Player player, String name) {
        this.x = startX;
        this.y = startY;
        this.spawnX = startX;
        this.spawnY = startY;
        this.player = player;
        this.npcName = name;
        this.allTextures = new Array<>();
        wanderTargetX = x;
        wanderTargetY = y;

        idleTextures = loadTextures(npcFolder);
        chaseTextures = loadTextures(npcFolder + "/run");
        attackTextures = loadTextures(npcFolder + "/attack");
        dieTextures = loadTextures(npcFolder + "/die");

        currentFrame = idleTextures.get("S");
    }

    private Map<String, TextureRegion> loadTextures(String path) {
        Map<String, TextureRegion> map = new HashMap<>();
        for (int i = 0; i < DIRS.length; i++) {
            Texture tex = new Texture(path + "/" + FILE_NAMES[i] + ".png");
            allTextures.add(tex);
            map.put(DIRS[i], new TextureRegion(tex));
        }
        return map;
    }

    public void setCollisionChecker(Player.CollisionChecker checker) {
        this.collisionChecker = checker;
    }

    public void setWorldBounds(float width, float height) {
        this.worldWidth = width;
        this.worldHeight = height;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return currentFrame != null ? currentFrame.getRegionWidth() : 0; }
    public float getHeight() { return currentFrame != null ? currentFrame.getRegionHeight() : 0; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public State getState() { return state; }
    public boolean isDying() { return state == State.DYING; }
    public boolean isDead() { return fullyDead; }
    public boolean isAlive() { return !fullyDead && state != State.DYING; }

    public boolean takeDamage(int amount) {
        if (fullyDead || state == State.DYING) return false;
        health = Math.max(0, health - amount);

        if (health <= 0) {
            state = State.DYING;
            deathTimer = deathDuration;
            return true;
        }

        if (state == State.WANDER || state == State.ALERT) {
            state = State.CHASE;
            lastKnownPlayerX = player.getX();
            lastKnownPlayerY = player.getY();
        }
        return false;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        this.spawnX = x;
        this.spawnY = y;
    }

    public void update(float delta) {
        if (fullyDead) return;

        if (state == State.DYING) {
            deathTimer -= delta;
            if (deathTimer <= 0f) fullyDead = true;
            return;
        }

        stateTime += delta;
        attackTimer = Math.max(0, attackTimer - delta);

        float hpRatio = (float)health / maxHealth;

        if (state != State.FLEE && hpRatio < fleeThreshold) {
            prevState = state;
            state = State.FLEE;
        } else if (state == State.FLEE && hpRatio > fleeThreshold + 0.15f) {
            state = prevState == State.WANDER ? State.WANDER : State.SEARCH;
            if (state == State.SEARCH) {
                lastKnownPlayerX = player.getX();
                lastKnownPlayerY = player.getY();
            }
        }

        switch (state) {
            case WANDER: updateWander(delta); break;
            case ALERT: updateAlert(delta); break;
            case CHASE: updateChase(delta); break;
            case ATTACK: updateAttack(delta); break;
            case FLEE: updateFlee(delta); break;
            case SEARCH: updateSearch(delta); break;
            default: break;
        }

        updateCurrentFrame();
    }

    private void updateWander(float delta) {
        float dist = distToPlayer();
        if (dist < aggroRange) {
            state = State.ALERT;
            alertTimer = 0f;
            lastKnownPlayerX = player.getX();
            lastKnownPlayerY = player.getY();
            return;
        }

        if (!isWanderMoving) {
            wanderPauseTimer -= delta;
            if (wanderPauseTimer <= 0f) {
                pickWanderTarget();
                isWanderMoving = true;
            }
        } else {
            float tx = wanderTargetX - x;
            float ty = wanderTargetY - y;
            float targetDist = (float)Math.sqrt(tx * tx + ty * ty);

            if (targetDist < 8f) {
                isWanderMoving = false;
                wanderPauseTimer = 1.0f + MathUtils.random(2.0f);
            } else {
                tx /= targetDist;
                ty /= targetDist;
                setDirection(tx, ty);
                moveInDirection(tx, ty, speed * 0.5f, delta);
            }
        }

        if (!isWanderMoving) {
            lastDirection = IDLE_DIRS[MathUtils.random(IDLE_DIRS.length - 1)];
        }
    }

    private void pickWanderTarget() {
        for (int attempt = 0; attempt < 10; attempt++) {
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float radius = 30f + MathUtils.random(wanderRadius);
            float tx = spawnX + MathUtils.cos(angle) * radius;
            float ty = spawnY + MathUtils.sin(angle) * radius;

            if (tx < 0 || tx >= worldWidth || ty < 0 || ty >= worldHeight) continue;

            int tileX = (int)(tx / 32);
            int tileY = (int)(ty / 32);
            if (collisionChecker != null && !collisionChecker.isBlocked(tileX, tileY)) {
                wanderTargetX = tx;
                wanderTargetY = ty;
                return;
            }
        }

        float angle = MathUtils.random(0f, MathUtils.PI2);
        float radius = 30f + MathUtils.random(wanderRadius);
        wanderTargetX = spawnX + MathUtils.cos(angle) * radius;
        wanderTargetY = spawnY + MathUtils.sin(angle) * radius;
    }

    private void updateAlert(float delta) {
        float dist = distToPlayer();
        lastKnownPlayerX = player.getX();
        lastKnownPlayerY = player.getY();

        if (dist > aggroRange * 1.2f) {
            alertTimer += delta;
            if (alertTimer > maxAlertTime) {
                state = State.WANDER;
                return;
            }
        } else {
            alertTimer = 0f;
        }

        float dx = player.getX() - x;
        float dy = player.getY() - y;
        setDirection(dx, dy);
    }

    private void updateChase(float delta) {
        float dist = distToPlayer();
        lastKnownPlayerX = player.getX();
        lastKnownPlayerY = player.getY();

        if (dist < attackRange) {
            state = State.ATTACK;
            attackTimer = 0f;
            return;
        }

        if (dist > deaggroRange) {
            state = State.SEARCH;
            searchTimer = 0f;
            searchReached = false;
            return;
        }

        float dx = player.getX() - x;
        float dy = player.getY() - y;
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        dx /= len;
        dy /= len;

        setDirection(dx, dy);
        moveInDirection(dx, dy, speed, delta);
    }

    private void updateAttack(float delta) {
        float dist = distToPlayer();
        lastKnownPlayerX = player.getX();
        lastKnownPlayerY = player.getY();

        if (dist > attackRange * 1.5f) {
            state = State.CHASE;
            return;
        }

        float dx = player.getX() - x;
        float dy = player.getY() - y;
        setDirection(dx, dy);

        if (dist > attackRange * 0.8f) {
            float len = (float)Math.sqrt(dx * dx + dy * dy);
            moveInDirection(dx / len, dy / len, speed * 0.6f, delta);
        }

        if (attackTimer <= 0f) {
            player.takeDamage(attackDamage);
            attackTimer = attackCooldown;
        }
    }

    private void updateFlee(float delta) {
        float dist = distToPlayer();

        if (dist > deaggroRange * 1.2f) {
            state = State.WANDER;
            return;
        }

        float dx = x - player.getX();
        float dy = y - player.getY();
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        dx /= len;
        dy /= len;

        setDirection(dx, dy);
        moveInDirection(dx, dy, speed * 1.2f, delta);
    }

    private void updateSearch(float delta) {
        float dist = distToPlayer();
        if (dist < aggroRange) {
            state = State.CHASE;
            return;
        }

        if (!searchReached) {
            float dx = lastKnownPlayerX - x;
            float dy = lastKnownPlayerY - y;
            float targetDist = (float)Math.sqrt(dx * dx + dy * dy);

            if (targetDist < 16f) {
                searchReached = true;
                searchTimer = 0f;
            } else {
                dx /= targetDist;
                dy /= targetDist;
                setDirection(dx, dy);
                moveInDirection(dx, dy, speed * 0.7f, delta);
            }
        } else {
            searchTimer += delta;
            if (searchTimer > maxSearchTime) state = State.WANDER;
        }
    }

    private void setDirection(float dx, float dy) {
        if (dy > 0.5f) {
            if (dx > 0.5f) lastDirection = "NE";
            else if (dx < -0.5f) lastDirection = "NW";
            else lastDirection = "N";
        } else if (dy < -0.5f) {
            if (dx > 0.5f) lastDirection = "SE";
            else if (dx < -0.5f) lastDirection = "SW";
            else lastDirection = "S";
        } else {
            if (dx > 0) lastDirection = "E";
            else if (dx < 0) lastDirection = "W";
        }
    }

    private void updateCurrentFrame() {
        if (state == State.DYING) {
            TextureRegion f = dieTextures.get(lastDirection);
            if (f != null) currentFrame = f;
            return;
        }

        TextureRegion frame = null;
        switch (state) {
            case WANDER:
            case ALERT:
            case SEARCH:
                frame = idleTextures.get(lastDirection); break;
            case CHASE:
            case FLEE:
                frame = chaseTextures.get(lastDirection); break;
            case ATTACK:
                frame = attackTextures.get(lastDirection); break;
            default: break;
        }
        if (frame != null) currentFrame = frame;
    }

    private float distToPlayer() {
        float dx = player.getX() - x;
        float dy = player.getY() - y;
        return (float)Math.sqrt(dx * dx + dy * dy);
    }

    private void moveInDirection(float dx, float dy, float moveSpeed, float delta) {
        float newX = x + dx * moveSpeed * delta;
        float newY = y + dy * moveSpeed * delta;

        float cx = colOffsetX();
        float cy = colOffsetY();
        float newCx = newX + cx;
        float newCy = newY + cy;

        if (collisionChecker != null) {
            boolean blockedX = isBlockedAt(newCx, y + cy);
            boolean blockedY = isBlockedAt(x + cx, newCy);

            if (blockedX && blockedY) {
                float perpX = -dy;
                float perpY = dx;
                float slideX = x + perpX * moveSpeed * delta;
                float slideY = y + perpY * moveSpeed * delta;
                if (!isBlockedAt(slideX + cx, y + cy)) x = slideX;
                else if (!isBlockedAt(x + cx, slideY + cy)) y = slideY;
            } else {
                if (!blockedX) x = newX;
                if (!blockedY) y = newY;
            }
        } else {
            x = newX;
            y = newY;
        }

        clampPosition();
    }

    private float visualOffsetY() {
        if (state == State.DYING) {
            float progress = 1f - deathTimer / deathDuration;
            return -progress * 10f + 1.5f * MathUtils.sin(stateTime * 18f);
        }
        switch (state) {
            case WANDER:
                if (isWanderMoving) return 2f * MathUtils.sin(stateTime * 8f);
                return 1.2f * MathUtils.sin(stateTime * 2.2f);
            case ALERT: return 1.5f * MathUtils.sin(stateTime * 2.5f);
            case CHASE:
            case FLEE: return 2.5f * MathUtils.sin(stateTime * 10f);
            case ATTACK: return 1.5f * MathUtils.sin(stateTime * 18f);
            case SEARCH:
                if (!searchReached) return 2f * MathUtils.sin(stateTime * 8f);
                return 1.2f * MathUtils.sin(stateTime * 2.2f);
            default: return 0f;
        }
    }

    private float visualOffsetX() {
        switch (state) {
            case WANDER:
                if (isWanderMoving) return 1f * MathUtils.sin(stateTime * 7f);
                return 0f;
            case CHASE:
            case FLEE: return 1.2f * MathUtils.sin(stateTime * 8f);
            default: return 0f;
        }
    }

    public boolean isHostile() {
        return state == State.CHASE || state == State.ATTACK || state == State.FLEE;
    }

    public void draw(SpriteBatch batch) {
        if (fullyDead) return;
        float w = currentFrame.getRegionWidth() * spriteScale;
        float h = currentFrame.getRegionHeight() * spriteScale;
        float drawX = x + visualOffsetX();
        float drawY = y + visualOffsetY();
        batch.draw(currentFrame, drawX, drawY, w, h);
    }

    public void drawName(SpriteBatch batch, BitmapFont font) {
        if (fullyDead) return;
        float w = currentFrame.getRegionWidth() * spriteScale;
        float h = currentFrame.getRegionHeight() * spriteScale;
        float visualY = y + h + 16 + visualOffsetY();
        float visualX = x + visualOffsetX();

        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, npcName);
        float textX = visualX + (w - layout.width) / 2f;

        if (state == State.DYING) font.setColor(0.6f, 0.2f, 0.2f, 0.6f);
        else if (isHostile()) font.setColor(1f, 0.3f, 0.3f, 1f);
        else font.setColor(1, 1, 0.5f, 1);
        font.draw(batch, npcName, textX, visualY);
    }

    public void drawHpBar(ShapeRenderer shapeRenderer) {
        if (state == State.DYING || fullyDead) return;
        float w = currentFrame.getRegionWidth() * spriteScale;
        float h = currentFrame.getRegionHeight() * spriteScale;
        float barW = 40;
        float barH = 4;
        float barX = x + (w - barW) / 2f + visualOffsetX();
        float barY = y + h + 10 + visualOffsetY();

        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(barX, barY, barW, barH);

        float fill = (float)health / maxHealth;
        float r = 0.2f + 0.8f * (1 - fill);
        float g = 0.8f * fill;
        shapeRenderer.setColor(r, g, 0.1f, 1);
        shapeRenderer.rect(barX, barY, barW * fill, barH);
    }

    private void clampPosition() {
        float w = currentFrame.getRegionWidth() * spriteScale;
        float h = currentFrame.getRegionHeight() * spriteScale;
        x = MathUtils.clamp(x, 0, worldWidth - w);
        y = MathUtils.clamp(y, 0, worldHeight - h);
    }

    private float colOffsetX() {
        return (currentFrame.getRegionWidth() * spriteScale - colWidth) / 2f;
    }
    private float colOffsetY() { return 0f; }

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

    public void dispose() {
        for (Texture t : allTextures) t.dispose();
    }
}
