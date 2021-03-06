package ua.andoroidflappybird.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import ua.andoroidflappybird.Bird;
import ua.andoroidflappybird.MaxScore;
import ua.andoroidflappybird.Pipe;
import ua.andoroidflappybird.Statements;
import ua.andoroidflappybird.neuralnetwork.NeuralNetwork;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FlappyBird implements Screen{
    private int population = 2000;
    private int living =population;

    private Statements st;
    private Bird[] birds;
    private List<Pipe> pipes;
    private ShapeRenderer sr;
    private SpriteBatch batch;
    private Stage stage;
    private OrthographicCamera camera;
    private int currPipeIndex = 0;
    private Label genLabel;
    private Label scoreLabel;
    private Label livingLabel;
    private Label maxScoreLabel;

    private Texture birdTexture;

    private MaxScore maxScore;

    private int passedPipes = 0;
    private int maxPassedPipes = 0;

    private int times = 1;

    private boolean superSpeedMode = false;

    public FlappyBird(Statements st) {
        this.st = st;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false,1920,1080);
        stage = new Stage(new FitViewport(1920, 1080));
        sr = new ShapeRenderer();
        sr.setProjectionMatrix(camera.combined);
        maxScore = new MaxScore();
        maxScore.load(new File(Gdx.files.internal("maxScore.json").path()));
        birds = new Bird[population];
        for(int i=0;i<population;i++) {
            birds[i] = new Bird(new File(Gdx.files.internal("bestBird.json").path()));
            if(i!= 0)
                birds[i].mutate(0.1f);
        }
        pipes = new ArrayList<>();
        pipes.add(new Pipe());
        batch.setProjectionMatrix(camera.combined);
        Skin skin = new Skin(Gdx.files.internal("skin/flat-earth-ui.json"));
        scoreLabel = new Label("0",skin, "title");
        scoreLabel.setPosition(10,10);
        maxScoreLabel = new Label("0",skin, "title");
        maxScoreLabel.setPosition(1920/2f,10);
        livingLabel = new Label("0",skin, "title");
        livingLabel.setPosition(10,1080-livingLabel.getHeight()-10);
        genLabel = new Label("0",skin, "title");
        genLabel.setPosition(1920/2f, 1080-livingLabel.getHeight()-10);

        Gdx.input.setInputProcessor(stage);
        birdTexture = new Texture(Gdx.files.internal("bird1.png"));
    }

    @Override
    public void render(float delta) {
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {times = 1;superSpeedMode = false;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {times = 2;superSpeedMode = false;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {times = 3;superSpeedMode = false;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {times = 4;superSpeedMode = false;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {times = 5;superSpeedMode = false;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {times = 10000/living;superSpeedMode = true;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_0)) {times = 0;superSpeedMode = false;}
        int prevTimes = times;
        while (prevTimes-- > 0)
            update(delta);


        Gdx.gl.glClearColor(1,1, 1,1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0,0,0,0.5f);
        sr.setColor(0,1,0f,1);
        for (Pipe p : pipes) p.render(sr);
        sr.end();
        batch.begin();
        for(Bird b : birds ) b.render(batch,birdTexture);
        genLabel.draw(batch,1);
        livingLabel.draw(batch,1);
        maxScoreLabel.draw(batch,1);
        scoreLabel.draw(batch,1);
        batch.end();
        stage.act(delta);
        stage.draw();
    }

    private void update(float delta) {
        if(superSpeedMode) times = 10000/living;
        maxScore.updateHighScore(passedPipes);
        if(passedPipes > maxPassedPipes) maxPassedPipes = passedPipes;

        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Bird max = birds[0];
            for(int i=1;i<population;i++)
                if(max.getScore() < birds[i].getScore())
                    max = birds[i];
            if(max.getScore() > maxScore.getScore()) {
                maxScore.setScore(max.getScore());
                save(max);
            }
            Gdx.app.exit();
        }

        if(living == 0) {
            Bird max = birds[0];
            for(int i=1;i<population;i++)
                if(max.getScore() < birds[i].getScore())
                    max = birds[i];
            if(max.getScore() > maxScore.getScore()) {
                maxScore.setScore(max.getScore());
                save(max);
            }
            for(int i=0;i<population;i++) {
                birds[i] = new Bird(new File(Gdx.files.internal("bestBird.json").path()));
                if(i!=0)
                    birds[i].mutate(1f);
            }
            restart();
        }
        for(int i=0;i<population;i++)
            birds[i].update(delta,pipes.get(currPipeIndex).getPipe1(),pipes.get(currPipeIndex).getPipe2(),pipes.get(currPipeIndex).getPipeX(), pipes.get(currPipeIndex).getWidth());
        List<Pipe> remove = new ArrayList<>();

        for (Pipe p : pipes){
            p.update(delta);
            if(p.isOff) remove.add(p);
        }

        for (Pipe p : remove) {
            pipes.remove(p);
            currPipeIndex--;
        }

        if(pipes.get(currPipeIndex).getPipeX()+pipes.get(currPipeIndex).getWidth() < 1920/6f) {
            currPipeIndex++;
            passedPipes++;
        }

        if(pipes.get(pipes.size()-1).getPipeX() < 3*1920/5f) pipes.add(new Pipe());
        for(int i=0;i<population;i++) {
            if ((isCollision(pipes.get(currPipeIndex), birds[i]) || birds[i].getY() < birds[i].getSize()/2f || birds[i].getY() > 1080 - birds[i].getSize()/2f) && !birds[i].isDied()) {
                birds[i].setDied(true);
                living--;
            }
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            File bb = new File(Gdx.files.internal("bestBird.json").path());
            File ms = new File(Gdx.files.internal("maxScore.json").path());
            try {
                FileWriter writerbb = new FileWriter(bb);
                FileWriter writerms = new FileWriter(ms);
                writerbb.flush();
                writerbb.close();
                writerms.flush();
                writerbb.close();
            } catch (IOException ignored) {}

            if(Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT)) {
                Gdx.app.exit();
            }
            maxScore = new MaxScore();
            restart();
            birds = new Bird[population];
            for(int i=0;i<population;i++) {
                birds[i] = new Bird(new File(Gdx.files.internal("bestBird.json").path()));
                if(i!= 0)
                    birds[i].mutate(0.1f);
            }
        }



        scoreLabel.setText("Score " + passedPipes);
        maxScoreLabel.setText("Max score " + maxScore.getHighScore());
        genLabel.setText("Gen " + maxScore.getGen());
        livingLabel.setText("Living " + living);
    }

    private void restart() {
        pipes.clear();
        pipes.add(new Pipe());
        if(maxPassedPipes < passedPipes) maxPassedPipes = passedPipes;
        passedPipes = 0;
        maxScore.setGen(maxScore.getGen()+1);
        currPipeIndex = 0;
        living = population;
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }

    private boolean isCollision(Pipe pipe, Bird bird) {
        Rect rect1 = new Rect(pipe.getPipeX(),pipe.getPipe1(),pipe.getWidth(),1080);
        Rect rect2 = new Rect(pipe.getPipeX(),pipe.getPipe2() - 1080,pipe.getWidth(),1080);
        Rect rect3 = new Rect(1920/6f - bird.getSize()/2f,bird.getY() - bird.getSize()/2f,bird.getSize(),bird.getSize());
        if (rect3.x < rect1.x + rect1.width &&
                rect3.x + rect3.width > rect1.x &&
                rect3.y < rect1.y + rect1.height &&
                rect3.y + rect3.height > rect1.y) {
            return true;
        }
        return rect3.x < rect2.x + rect2.width &&
                rect3.x + rect3.width > rect2.x &&
                rect3.y < rect2.y + rect2.height &&
                rect3.y + rect3.height > rect2.y;
    }

    public class Rect {
        float x,y,width,height;
        Rect (float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h; 
        }
    }

    public void save(Bird bird) {
        NeuralNetwork brain = bird.getBrain();
        brain.setFile(new File(Gdx.files.internal("bestBird.json").path()));
        try {
            brain.save();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        maxScore.save(new File(Gdx.files.internal("maxScore.json").path()));
    }
}
