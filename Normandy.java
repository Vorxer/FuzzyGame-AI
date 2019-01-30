/*
 * SimpleController.java
 *
 * Created on 17 March 2008, 17:18
 *
 */

package battle.saucers.controllers;

import battle.Constants;
import battle.sensors.SensorData;
import java.awt.Color;
import java.util.ArrayList;
import battle.saucers.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *
 * @author phingsto
 * 
 * SimpleController
 *  - cruises at a moderate speed
 *  - tries to dodge close photon blasts (also uses shields if has lots of energy)
 *  - otherwise always tries to get to the closest power-up at max speed
 *  - fires 1% of the time, always at closest enemy
 *  - if there is no power-up or nearby photon blast, turns random amounts at random intervals
 * 
 */
public class Normandy implements SaucerController, Constants ,KeyListener
{
    private static final String NAME = "Normandy";
    private static final Color BASE = Color.white;
    private static final Color ARROW = Color.black;
    private static final double FIRE_PROB = 0.01;
    private static final double TURN = 3.0;
    private static final double FIRE_POWER = SAUCER_MAX_POWER;
    private static final int TIME_BETWEEN_TURNS = 30;
    
    
    private SensorData target;
    private SensorData nearestBlast;
    private double energy;
    private boolean dodgeBlast = false;
    private boolean powerUpOn;
    private double nearestPowerUpDirection;
    private double timeUntilNextTurn = TIME_BETWEEN_TURNS;
    private double speed;
    private boolean openFire;
    private boolean shieldsUp;
    private boolean nowFiring;
    private double firepower=10;
    private double rudderShift;
    private double lock;
    private static final double speeControlSensitivity =2.5;
    private static final double directionControlSensitivity =3;
    private static final double firePowerControlSensitivity =1;
    private String message = "";
    JProgressBar firePowerBar ;
    // Declare your FuzzyVariables and rule set(s) here
    
    public Normandy()  throws Exception
    {
        
        JFrame inputWindow = new JFrame("Normandy Combat Information Centre"); 
        inputWindow.setFocusTraversalKeysEnabled(false);
        inputWindow.addKeyListener(this);
        //JLabel textlabel = new JLabel("Select Window to Control Normandy",SwingConstants.CENTER);
        //textlabel.setPreferredSize(new Dimension(300,100));
        //inputWindow.getContentPane().add(textlabel,BorderLayout.CENTER);
        
        
        speedBar = new javax.swing.JProgressBar();
        chargeBar = new javax.swing.JProgressBar();
        relativeBar = new javax.swing.JProgressBar();
        actualBar = new javax.swing.JProgressBar();
        
        topLabel1 = new javax.swing.JLabel();
        topLabel2 = new javax.swing.JLabel();
        
        speedLabel = new javax.swing.JLabel();
        chargeLabel = new javax.swing.JLabel();
        relativeLabel = new javax.swing.JLabel();
        actualLabel = new javax.swing.JLabel();
        
        
        GridLayout CICLayout = new GridLayout(5,2);
        
        final JPanel CICPanel = new JPanel();
        
        CICPanel.setLayout(CICLayout);
        
        topLabel1.setFont(new java.awt.Font("Arial Rounded MT Bold", 0, 16));
        topLabel1.setText("Select Window to");
        
        topLabel1.setHorizontalAlignment(SwingConstants.RIGHT);

        
        topLabel2.setFont(new java.awt.Font("Arial Rounded MT Bold", 0, 16)); 
        topLabel2.setText(" Control Normandy");    
        
        speedLabel.setFont(new java.awt.Font("Arial Rounded MT Bold", 0, 14)); 
        speedLabel.setText("Speed");

        chargeLabel.setFont(new java.awt.Font("Arial Rounded MT Bold", 0, 14)); 
        chargeLabel.setText("Charge");

        relativeLabel.setFont(new java.awt.Font("Arial Rounded MT Bold", 0, 14)); 
        relativeLabel.setText("Relative");

        actualLabel.setFont(new java.awt.Font("Arial Rounded MT Bold", 0, 14)); 
        actualLabel.setText("Health");
        
        speedBar.setMaximum(250);
        speedBar.setMinimum(0);
        //speedBar.setBackground(Color.white);
        speedBar.setForeground(Color.red);
        
        chargeBar.setMaximum(100);
        chargeBar.setMinimum(0);
        chargeBar.setForeground(Color.blue);
        
        relativeBar.setMaximum(200);
        relativeBar.setMinimum(0);
        
        
        actualBar.setMaximum(100);
        actualBar.setMinimum(0);
        
        
        
        
        CICPanel.add(topLabel1);
        CICPanel.add(topLabel2);
        CICPanel.add(speedLabel);
        CICPanel.add(speedBar);
        CICPanel.add(chargeLabel);
        CICPanel.add(chargeBar);
        CICPanel.add(relativeLabel);      
        CICPanel.add(relativeBar);
        CICPanel.add(actualLabel);
        CICPanel.add(actualBar);
        
        inputWindow.getContentPane().add(CICPanel);
        
        
        
        inputWindow.setLocationRelativeTo(null);
        inputWindow.pack();
        inputWindow.setVisible(true);
        

             // Create your FuzzyVariables and rule set(s) here
    }
    
        public void sensorUpdate()
    {
        //System.out.println("firepower = "+firepower);
        //System.out.println("rudder shift = "+rudderShift);
        //System.out.println("speed = "+speed);
        
        nowFiring=false;
        int speedInt = (int) speed;
        speedBar.setValue(speedInt);
        
        int chargeInt = (int) firepower;
        chargeBar.setValue(chargeInt);
        
        int localEnergyInt = (int) energy;
        int opponentEnergyInt = (int) target.energy;
               
        int actualInt = (localEnergyInt);
        int actualBarValue=(actualInt+1)/100;
        
        
        if (actualBarValue >= 60)
        {
            actualBar.setForeground(Color.green);
        }
        
        if (actualBarValue < 65)
        {
            actualBar.setForeground(Color.ORANGE);
        }
        
        if (actualBarValue < 30)
        {
            actualBar.setForeground(Color.red);
        }
        
        actualBar.setValue(actualBarValue);
        
        
        double relativePercent = (((energy)/(target.energy+1))*100);
        
        if (relativePercent >= 200)
        {
            relativePercent=200;
        }
        
        if (relativePercent < 5)
        {
            relativePercent=5;
        }
               
        int relativeInt = (int) relativePercent;
        
        
        if (relativeInt >= 125)
        {
            relativeBar.setForeground(Color.green);
        }
        
        if (relativeInt < 125)
        {
            relativeBar.setForeground(Color.ORANGE);
        }
        
        if (relativeInt < 75)
        {
            relativeBar.setForeground(Color.red);
        }
        
        relativeBar.setValue(relativeInt);
        

        

        
        
        //actualBar.setForeground(Color.green);
        //System.out.println("relativePercent = "+relativePercent);
        
    }
    
        public void keyPressed(KeyEvent e)
    {
        
        int keyCode = e.getKeyCode();
        if(keyCode == KeyEvent.VK_W && speed !=250)
        {
            speed=speed+(10*speeControlSensitivity);
        }
        
        if(keyCode == KeyEvent.VK_A)
        {
            rudderShift=1*directionControlSensitivity;
        }
                
        if(keyCode == KeyEvent.VK_D)
        {
            rudderShift=-1*directionControlSensitivity;
        }
        
        if(keyCode == KeyEvent.VK_S && speed !=25)
        {
            speed=speed-(10*speeControlSensitivity);
        }
        
        if(keyCode == KeyEvent.VK_SPACE)
        {
            openFire=true;
        }
        
        if(keyCode == KeyEvent.VK_ENTER)
        {
            shieldsUp=true;
        }
        
        if(keyCode == KeyEvent.VK_SHIFT && firepower != 100)
        {
            firepower=firepower+10;
        }
        
        if(keyCode == KeyEvent.VK_CONTROL && firepower !=10)
        {
            firepower=firepower-10;
        }
        
        
    }
    
    public void keyTyped(KeyEvent e)
    {
        
            
    }
    
    public void keyReleased(KeyEvent e)
    {
        int keyCode = e.getKeyCode();
        if(keyCode == KeyEvent.VK_D)
        {
            rudderShift=0;
        }
                
        if(keyCode == KeyEvent.VK_A)
        {
            rudderShift=0;
        }
        
        if(keyCode == KeyEvent.VK_SPACE)
        {
            openFire=false;
        }
        
        if(keyCode == KeyEvent.VK_ENTER)
        {
            shieldsUp=false;
        }
      
    }
        
    public void senseSaucers(ArrayList<SensorData> data) 
    {
        // This is where you get told about enemies
        // save whatever information you need in suitable member variables
        
        // find the closest enemy to target - this will be used later in getTarget()
        if(data.size() > 0)
        {
            double closest = data.get(0).distance;
            target = data.get(0);
            for(SensorData thisData: data)
            {
                if(thisData.distance < closest)
                {
                    target = thisData;
                    closest = thisData.distance;
                }
            }
        }
        else
        {
            target = null;
        }
    }

    public void sensePowerUps(ArrayList<SensorData> data) 
    {
        // This is where you get told about power-ups
        // save whatever information you need in suitable member variables
        
        // find the closest powerUp
        powerUpOn = data.size() > 0;
        if(powerUpOn)
        {
            double closest = data.get(0).distance;
            nearestPowerUpDirection = data.get(0).direction;
            for(SensorData thisData: data)
            {
                if(thisData.distance < closest)
                {
                    nearestPowerUpDirection = thisData.direction;
                    closest = thisData.distance;
                }
            }
        }
    }
    
    private ArrayList<SensorData> blasts;

     public void senseBlasts(ArrayList<SensorData> data) 
    {
        blasts = data;
        
        // This is where you get told about photon blasts
        // save whatever information you need in suitable member variables
        
        // find the closest blast
        if(data.size() > 0)
        {
            double closest = data.get(0).distance;
            nearestBlast = data.get(0);
            for(SensorData thisData: data)
            {
                if(thisData.distance < closest)
                {
                    nearestBlast = thisData;
                    closest = thisData.distance;
                }
            }
            
            dodgeBlast = closest < STARFIELD_WIDTH/10;            
        }
        else
        {
            nearestBlast = null;
            dodgeBlast = false;
        }
    }
     
    public void senseEnergy(double energy)
    {
        // This is where you get told what your own energy level is
        // Save this information if you need it
        
       this.energy = energy;
    }
    
    // methods below determine what your saucer does

    // fires at random intervals
    public SensorData getTarget() 
    {      sensorUpdate();  

            return target;

    }

    // always uses max power if it fires
    public double getFirePower()
    {
        sensorUpdate(); 
        if(openFire == true)
        {
            return firepower;
        }

        
        return 0.0;
    }
    
    // turns a random amount between -3 and +3 degrees each turn
    // or heads for the nearest powerUp
        public double getTurn()
    {
        sensorUpdate(); 
        return rudderShift;
    }
    
    public double getSpeed()
    {
        sensorUpdate(); 
        return speed;
    }
    
    public boolean getShields()
    {
        sensorUpdate(); 
        return shieldsUp;
    }
    
    public String getName()
    {
        return NAME;
    }
    
    public Color getBaseColor()
    {
        return BASE;
    }
    
    public Color getTurretColor()
    {
        return ARROW;
    }
    
    private javax.swing.JLabel topLabel1;
    private javax.swing.JLabel topLabel2;
    private javax.swing.JLabel speedLabel;
    private javax.swing.JLabel chargeLabel;
    private javax.swing.JLabel relativeLabel;
    private javax.swing.JLabel actualLabel;
    private javax.swing.JProgressBar speedBar;
    private javax.swing.JProgressBar chargeBar;
    private javax.swing.JProgressBar relativeBar;
    private javax.swing.JProgressBar actualBar;
}
