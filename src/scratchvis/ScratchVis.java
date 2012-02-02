package scratchvis;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PGraphics;
import oscP5.*;
import netP5.*;
import ddf.minim.*;
import java.util.*;

public class ScratchVis extends PApplet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	OscP5 oscP5;
	float rTime,lTime;
	int lastXRight = -1;
	int lastYRight = -1;
	int lastXLeft = -1;
	int lastYLeft = -1;
	float lastFader = 0.5f;
	int hitTimer = 0;
	PGraphics recordLeft, recordRight, tempRight, tempLeft;//, screenTemp;
	Minim minim;
	AudioInput in;
	static final int SCREEN_WIDTH = 400;
	static final int SCREEN_HEIGHT = 400;
	int halfHeight = SCREEN_HEIGHT/2;
	int halfWidth = SCREEN_WIDTH/2;
	int quartHeight = SCREEN_HEIGHT/4;
	int quartWidth = SCREEN_WIDTH/4;
	ArrayList<Particle> particles;
	boolean gotLeftData = false;

	public void setup() {
		particles = new ArrayList<Particle>();
		size(SCREEN_WIDTH,SCREEN_HEIGHT);
		frameRate(30);
		imageMode(CENTER);
		
		
		oscP5 = new OscP5(this, 8319);
		minim = new Minim(this);
		in = minim.getLineIn();
		recordLeft = createGraphics(SCREEN_WIDTH,SCREEN_HEIGHT,P2D);
		recordRight = createGraphics(SCREEN_WIDTH,SCREEN_HEIGHT,P2D);
		tempRight = createGraphics(SCREEN_WIDTH,SCREEN_HEIGHT,P2D);
		tempLeft = createGraphics(SCREEN_WIDTH,SCREEN_HEIGHT,P2D);
//		screenTemp = createGraphics(SCREEN_WIDTH,SCREEN_HEIGHT,P2D);
//		screenTemp.imageMode(CENTER);
	}
	
	public void update(){
	}
	
	private void drawRecord(PGraphics record, PGraphics temp, float rotation, int direction, int r, int g, int b){
		float rad = Math.min(halfHeight, halfWidth);
		int x = (int)(rad*Math.cos(rotation));
		int y = (int)(rad*Math.sin(rotation));
		if (direction > 0 && lastXRight == -1){
			lastXRight = x;
			lastYRight = y;
		} else if (direction < 0 && lastXLeft == -1){
			lastXLeft = x;
			lastXRight = y;
		}
		temp.beginDraw();
			temp.background(0,0);
			temp.pushMatrix();
				temp.translate(halfWidth, halfHeight);
				temp.scale(0.96f);
				temp.translate(-halfWidth,-halfHeight);
				temp.image(record.get(),0,0);
			temp.popMatrix();
			//temp.filter(BLUR, 2);
		temp.endDraw();
		record.beginDraw();
			record.background(0,0);
			record.image(temp.get(),0,0);
	//		recordRight.pushMatrix();
	//		recordRight.rotate((float)(-rotation+Math.PI/2));
			record.stroke(r,g,b,255);//(int)(Math.random()*255));
			record.strokeWeight(8);
			record.pushMatrix();
				record.translate(halfWidth, halfHeight);
				record.rotate(-(float)Math.PI/2);
				int lastX, lastY;
				if (direction > 0){
					lastX = lastXRight;
					lastY = lastYRight;
				} else {
					lastX = lastXLeft;
					lastY = lastYLeft;
				}
				record.strokeCap(ROUND);
				record.line(lastX, lastY, x, y);
			record.popMatrix();
			if (direction > 0){
				lastXRight = x;
				lastYRight = y;
			} else {
				lastXLeft = x;
				lastYLeft = y;
			}
		record.endDraw();
	}

	public void draw() {
		background(0);
		if (hitTimer != 0){
			boolean high = hitTimer > 0;
			//text("HIT", 200,(high ? 50 : 350));
			if (hitTimer == 5 || hitTimer == -5){
				while(particles.size() > 390){
					particles.remove(0);
				}
				int height = SCREEN_HEIGHT / 2;
				for(int i = 0; i< 3; i++){
					particles.add(new Particle(hitTimer > .5 ? 0 : SCREEN_WIDTH, height));
				}
			}
			hitTimer = hitTimer + (!high ? 1 : -1);
		}
		
		float rotation = (float)((rTime % (60000.0/(100.0/3)))/(60000.0/(100.0/3))*2*Math.PI);
		drawRecord(recordRight, tempRight, rotation, 1, 255, 100, 100);
		pushMatrix();
			translate(SCREEN_WIDTH,halfHeight);
			rotate(-rotation-(float)Math.PI/2);
			//scale(2);
			image(recordRight.get(), 0, 0);
		popMatrix();
		
		rotation = -(float)((lTime % (60000.0/(100.0/3)))/(60000.0/(100.0/3))*2*Math.PI);
		drawRecord(recordLeft, tempLeft, rotation, -1, 180, 180, 255);
//		screenTemp.beginDraw();
//			screenTemp.background(0,0);
//			screenTemp.pushMatrix();
//				screenTemp.translate(0,halfHeight);
//				screenTemp.rotate(-rotation+(float)Math.PI/2);
//				screenTemp.image(recordLeft.get(), 0, 0);
//			screenTemp.popMatrix();
//		screenTemp.endDraw();
		pushMatrix();
			translate(0, halfHeight);
			rotate(-rotation+(float)Math.PI/2);
			//scale(2);
			image(recordLeft.get(), 0, 0);
		popMatrix();
//		blend(screenTemp.get(), 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, DIFFERENCE);
		filter(BLUR, 7);
		
		float[] samples = in.mix.toArray();
//		if (samples.length > 0){
//			pushMatrix();
//			scale(400.0f/samples.length, 400);
//			fill(255);
//			stroke(255);
//			for(int i = 0; i < samples.length; i++){
//				point(i, samples[i]);
//			}
//			popMatrix();
//		}
		Particle p;
		fill(255);
		noStroke();
		int halfHeight = SCREEN_HEIGHT/2;
		int sampleLength = samples.length;
		for(int i = 0; i < particles.size();){
			p = particles.get(i);
			p.step();
			if (p.isVisible()){
				if (p.centered){
					p.y = halfHeight + samples[(int)(p.x/SCREEN_WIDTH*sampleLength)]*100;
				}
				ellipse(p.x, p.y, p.size, p.size);
				i++;
			} else {
				particles.remove(i);
			}
		}
	}
	
	public void oscEvent(OscMessage a_msg){
		if (a_msg.checkAddrPattern("/scratch/record/right")){
			rTime = a_msg.get(0).floatValue();
			if (!gotLeftData){
				lTime = rTime;
			}
		} else if (a_msg.checkAddrPattern("/scratch/record/left")){
			gotLeftData = true;
			lTime = a_msg.get(0).floatValue();
		} else if (a_msg.checkAddrPattern("/scratch/mixer/fader")){
			float curr = a_msg.get(0).floatValue();
			if (curr > .95 && lastFader <= .95){
				hitTimer = 5;
			} else if (curr < .05 && lastFader >= .05){
				hitTimer = -5;
			}
		}
	}
	
	public static void main(String _args[]) {
		PApplet.main(new String[] { scratchvis.ScratchVis.class.getName() });
	}
	
	class Particle{
		
		int size;
		float x;
		float y;
		float xV;
		float yV;
		boolean centered;
		boolean falling;
		int ttl = -1;
		
		public Particle(int x, int y){
			this.x = x;
			this.y = y;
			size = (int)(Math.random() * 20 + 5);
			yV = (float)Math.cos(Math.random()*2*Math.PI)*10;
			falling = yV > 0;
			xV = (float)(Math.random()*5 + 5);
			if (x > SCREEN_WIDTH / 2){
				xV = -xV;
			}
			if (size >= 12){
				ttl = (int)(Math.random()*60+100);
			}
		}
		
		public float getX(){
			return x;
		}
		
		public float getY(){
			return y;
		}
		
		public void setY(int y){
			this.y = y;
		}
		
		public int getSize(){
			return size;
		}
		
		public void step(){
			if (ttl > 0){
				ttl--;
			}
			x+=xV;
			if (Math.abs(xV) > .6){
				xV += (xV < 0 ? .1 : -.1);
			}
			if (!centered){
				y+=yV;
				yV += (falling ? -.4 : .4);
				if ((falling && y < SCREEN_HEIGHT/2) || (!falling && y > SCREEN_HEIGHT/2)){
					centered = true;
				}
			}
		}
		
		public boolean isVisible(){
			return x >= 0 && x <= SCREEN_WIDTH && (size < 12 || !centered);// && ttl != 0;
		}
		
		public boolean isCentered(){
			return centered;
		}
	}
}
