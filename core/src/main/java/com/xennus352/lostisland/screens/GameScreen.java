package com.xennus352.lostisland.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.xennus352.lostisland.LostIslandGame;
import com.xennus352.lostisland.entity.Player;

public class GameScreen implements Screen {

    // 🎥 ဂိမ်းရဲ့ ကင်မရာ view size
    private static final float WORLD_WIDTH = 800f;
    private static final float WORLD_HEIGHT = 600f;

    private final LostIslandGame game;
    private final String characterPath;
    private final String characterName;

    private SpriteBatch batch;
    private Player player;

    private OrthographicCamera camera;
    private Viewport viewport;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private int mapPixelWidth;
    private int mapPixelHeight;

    private ShapeRenderer debugRenderer;

    private static final String[] SOLID_LAYERS = {"Tree", "House"};
    private static final float ZOOM = 1f;

    public GameScreen(LostIslandGame game, String characterPath, String characterName) {
        this.game = game;
        this.characterPath = characterPath;
        this.characterName = characterName;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply();

        // 🗺️ updated to load Village.tmx instead of island.tmx
        map = new TmxMapLoader().load("Maps/Village.tmx");

        // မြေပုံအကွက်တွေ အချိုးအစားမပျက်စေဖို့ unitScale ကို 1f ထားပါတယ်
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f);

        debugRenderer = new ShapeRenderer();

        // Player ကို စတင်မယ့်နေရာမှာ နေရာချခြင်း
        player = new Player(400, 300, characterPath);

        // Store map dimensions for camera clamping
        mapPixelWidth = map.getProperties().get("width", Integer.class) * 32;
        mapPixelHeight = map.getProperties().get("height", Integer.class) * 32;
        player.setWorldBounds(mapPixelWidth, mapPixelHeight);

        camera.zoom = ZOOM;

        // Set up collision detection (tile-coordinate based)
        // Street tiles override solid layers — roads are always walkable
        // House layer: only block actual houses (GID 26–34, House.tsx / VillageHouse.png),
        //              not bridge/filler tiles (GID 35–46, House1.tsx / VillageHouse2.png)
        TiledMapTileLayer streetLayer = (TiledMapTileLayer) map.getLayers().get("Street");
        System.out.println("[DEBUG] Street layer found: " + (streetLayer != null));
        player.setCollisionChecker((tileX, tileY) -> {
            if (streetLayer != null) {
                TiledMapTileLayer.Cell streetCell = streetLayer.getCell(tileX, tileY);
                if (streetCell != null && streetCell.getTile() != null) return false;
            }
            for (String layerName : SOLID_LAYERS) {
                TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(layerName);
                if (layer == null) continue;
                TiledMapTileLayer.Cell cell = layer.getCell(tileX, tileY);
                if (cell == null || cell.getTile() == null) continue;
                // On the House layer only actual houses (GID 26–34) block
                if (layerName.equals("House")) {
                    int gid = cell.getTile().getId();
                    if (gid >= 26 && gid <= 34) return true;
                } else {
                    return true;
                }
            }
            return false;
        });

        // Slow down on Stream / River tiles, and on bridge tiles (House1, GID 35–46)
        final String[] slowLayers = {"Stream", "River"};
        player.setTileSpeedModifier((tileX, tileY) -> {
            for (String name : slowLayers) {
                TiledMapTileLayer l = (TiledMapTileLayer) map.getLayers().get(name);
                if (l == null) continue;
                TiledMapTileLayer.Cell c = l.getCell(tileX, tileY);
                if (c != null && c.getTile() != null) return 0.4f;
            }
            TiledMapTileLayer houseLayer = (TiledMapTileLayer) map.getLayers().get("House");
            if (houseLayer != null) {
                TiledMapTileLayer.Cell c = houseLayer.getCell(tileX, tileY);
                if (c != null && c.getTile() != null) {
                    int gid = c.getTile().getId();
                    if (gid >= 35 && gid <= 46) return 0.4f;
                }
            }
            return 1f;
        });

        // Move player to a safe spawn if current position is blocked
        findSafeSpawn();

        System.out.println("Selected Character : " + characterName);
    }

    private void findSafeSpawn() {
        TiledMapTileLayer ground = (TiledMapTileLayer) map.getLayers().get("Ground");
        if (ground == null) return;

        // First check if current spawn (400, 300) is already safe
        int tx = (int)Math.floor(player.getX() / 32);
        int ty = (int)Math.floor(player.getY() / 32);
        if (!isTileBlocked(tx, ty) && isAreaClear(tx, ty, 3)) return;

        // Find the most open area (most clear tiles in a 5x5 radius)
        int bestX = -1, bestY = -1, bestScore = -1;
        for (int y = 0; y < ground.getHeight(); y++) {
            for (int x = 0; x < ground.getWidth(); x++) {
                if (ground.getCell(x, y) == null || ground.getCell(x, y).getTile() == null)
                    continue;
                if (isTileBlocked(x, y)) continue;
                int score = 0;
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || nx >= ground.getWidth() || ny < 0 || ny >= ground.getHeight())
                            continue;
                        if (ground.getCell(nx, ny) == null || ground.getCell(nx, ny).getTile() == null)
                            continue;
                        if (!isTileBlocked(nx, ny)) score++;
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        if (bestX >= 0 && bestY >= 0)
            player.setPosition(bestX * 32, bestY * 32);
    }

    private boolean isAreaClear(int cx, int cy, int radius) {
        TiledMapTileLayer ground = (TiledMapTileLayer) map.getLayers().get("Ground");
        if (ground == null) return false;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int nx = cx + dx, ny = cy + dy;
                if (nx < 0 || nx >= ground.getWidth() || ny < 0 || ny >= ground.getHeight())
                    return false;
                if (ground.getCell(nx, ny) == null || ground.getCell(nx, ny).getTile() == null)
                    return false;
                if (isTileBlocked(nx, ny)) return false;
            }
        }
        return true;
    }

    private boolean isTileBlocked(int tileX, int tileY) {
        // Street tiles override all solid layers
        TiledMapTileLayer street = (TiledMapTileLayer) map.getLayers().get("Street");
        if (street != null) {
            TiledMapTileLayer.Cell sc = street.getCell(tileX, tileY);
            if (sc != null && sc.getTile() != null) return false;
        }
        for (String layerName : SOLID_LAYERS) {
            TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(layerName);
            if (layer == null) continue;
            TiledMapTileLayer.Cell cell = layer.getCell(tileX, tileY);
            if (cell != null && cell.getTile() != null) return true;
        }
        return false;
    }

    @Override
    public void render(float delta) {
        // Player ရဲ့ ရွေ့လျားမှုကို Update လုပ်မယ်
        player.update(delta);

        // 🎥 ကင်မရာကို Player ရဲ့ ဗဟိုချက်ဆီ လိုက်ခိုင်းခြင်း
        float playerCenterX = player.getX() + (player.getWidth() / 2f);
        float playerCenterY = player.getY() + (player.getHeight() / 2f);

        // Clamp camera so no black borders are visible outside the map
        float halfViewW = (camera.viewportWidth * camera.zoom) / 2f;
        float halfViewH = (camera.viewportHeight * camera.zoom) / 2f;
        float camX = MathUtils.clamp(playerCenterX, halfViewW, mapPixelWidth - halfViewW);
        float camY = MathUtils.clamp(playerCenterY, halfViewH, mapPixelHeight - halfViewH);

        camera.position.set(camX, camY, 0);
        camera.update();

        // နောက်ခံ screen ကို သန့်ရှင်းပေးခြင်း
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1);

        // ၁။ Tiled မြေပုံကို အရင်ဆွဲမယ်
        mapRenderer.setView(camera);
        mapRenderer.render();

        // ၂။ Player ကို မြေပုံအပေါ်ထပ်ကနေ ဆွဲမယ်
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.draw(batch);
        batch.end();

        // ၃။ Draw red outlines on collision tiles
        drawCollisionDebug();
    }

    private void drawCollisionDebug() {
        debugRenderer.setProjectionMatrix(camera.combined);
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);
        debugRenderer.setColor(Color.RED);

        TiledMapTileLayer street = (TiledMapTileLayer) map.getLayers().get("Street");
        for (String layerName : SOLID_LAYERS) {
            TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(layerName);
            if (layer == null) continue;
            for (int tx = 0; tx < layer.getWidth(); tx++) {
                for (int ty = 0; ty < layer.getHeight(); ty++) {
                    // Skip tiles overridden by a road
                    if (street != null) {
                        TiledMapTileLayer.Cell sc = street.getCell(tx, ty);
                        if (sc != null && sc.getTile() != null) continue;
                    }
                    TiledMapTileLayer.Cell cell = layer.getCell(tx, ty);
                    if (cell != null && cell.getTile() != null) {
                        debugRenderer.rect(tx * 32, ty * 32, 32, 32);
                    }
                }
            }
        }

        debugRenderer.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (player != null) player.dispose();
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (debugRenderer != null) debugRenderer.dispose();
    }
}
