package battle.saucers.controllers;


/*

Enterprise V 1.1.0 

      --Notes--
This version ported directly from Enterprise MK1
The following methods were ripped diectly from SimpleController
senseSaucers
sensePowerUps
senseBlasts
senseEnergy
getTarget
getShields
All the new get methods therefore work at Random


    --Changelog--
Added methods from simplecontroller
Added method updateRules() - to update the fuzzy rules

*/


import au.edu.ecu.is.fuzzy.*;
import battle.Constants;
import battle.saucers.Saucer;
import java.awt.Color;
import java.util.Random;
import java.util.ArrayList;
import battle.sensors.SensorData;
import java.util.Queue; 




public class Enterprise implements SaucerController
{
    // This will be the rule set
    
    
    private MamdaniRuleSet playStyleRules;
    private MamdaniRuleSet outputControl;
    
    
    // These will be the input variables
    
    private FuzzyVariable relativePower; // The power of the saucer in comparison to the opponent
    private FuzzyVariable actualPower;   // The power of the saucer as a percentage of total power
    private FuzzyVariable playStyle;     // The variable for style of play
    private FuzzyVariable range;         // The distance from the opponent
    private FuzzyVariable outputPower;   // The variable used to compute the amount of power put into each operation
    private FuzzyVariable accuracy;
    
    FuzzySet[] rpsets;
    FuzzySet[] apsets;
    FuzzySet[] currentPlaystyle;
    
    private static final Random random = new Random();
    private static final String NAME = "Enterprise";
    private static final Color BASE = Color.white;
    private static final Color ARROW = Color.red;
    public double opponentBearing;
    
    
    private SensorData target;
    private double nearestPowerUpDirection;
    private boolean powerUpOn;
    private SensorData nearestBlast;
    private boolean dodgeBlast = false;
    private double energy;
    private double deterioration;
    private double newValue;
    private double oldValue;
    private double foecount;
    private double enemyStats[];
    private double firepower;
    private ArrayList<Double> opponentHealths = new ArrayList<Double>();
    
    
    
    
    
    public Enterprise() throws FuzzyException, Exception
    {
        oldValue=10000;
        //create a variable for range, which will be used to put the distance from the target into perspective

         final double fullRange = Math.sqrt(Constants.STARFIELD_WIDTH*Constants.STARFIELD_WIDTH+
                    Constants.STARFIELD_HEIGHT*Constants.STARFIELD_HEIGHT);
        final double rangeIndex01 = 0.2*fullRange;
        final double rangeIndex02 = 0.3*fullRange;
        final double rangeIndex03 = 0.5*fullRange;
        final double rangeIndex04 = 0.6*fullRange;
        range = new FuzzyVariable("range", "m", 0.0, fullRange, 2);
        FuzzySet shortRange = new FuzzySet("Short Range", 0.0, 0.0, rangeIndex01, rangeIndex02);
        FuzzySet midRange = new FuzzySet("Medium Range", rangeIndex01, rangeIndex02, rangeIndex02, rangeIndex03);
        FuzzySet maxRange = new FuzzySet("Maximum Range", rangeIndex03, rangeIndex04, fullRange, fullRange);
        range.add(shortRange);
        range.add(midRange);
        range.add(maxRange);
        range.display();
         
        

        final double maxOutput = 10;
        outputPower = new FuzzyVariable("outputPower FP Var", "%", 0, 10, 2);
        FuzzySet highOutput = new FuzzySet("high", 5.0, 6, maxOutput, maxOutput);
        FuzzySet regularOutput = new FuzzySet("regularOutput", 2, 3, 7, 8);
        FuzzySet lowOutput = new FuzzySet("lowOutput", 0, 0, 4, 5);
        FuzzySet nullOutput = new FuzzySet("nullOutput", 0, 0, 0, 0);
        outputPower.add(highOutput);
        outputPower.add(regularOutput);
        outputPower.add(lowOutput);
        outputPower.add(nullOutput);
        outputPower.display();

        // create fuzzy variable relative power between the Saucer and the opponent as a percentage
        double maxrp = 1000000000;
        relativePower = new FuzzyVariable("relativepower", "%", 0, maxrp, 2);
        FuzzySet relativeLow = new FuzzySet("Low", 0, 10, 65, 75);
        FuzzySet relativeMedium = new FuzzySet("Medium", 65, 100, 120, 135);
        FuzzySet relativeHigh = new FuzzySet("High", 115, 140, maxrp, maxrp);
        relativePower.add(relativeLow);
        relativePower.add(relativeMedium);
        relativePower.add(relativeHigh);
        relativePower.display();
        relativePower.display();       
          
        
        // create fuzzy variable for current energy level as a percentage value
        actualPower = new FuzzyVariable("actual energy", "*", 0, 100, 2);
        FuzzySet high = new FuzzySet("high", 65, 80, 100, 100);
        FuzzySet medium = new FuzzySet("medium", 30, 45, 65, 70);
        FuzzySet low = new FuzzySet("low", 15, 20, 30, 35);
        FuzzySet critical = new FuzzySet("critical", 0, 0, 15, 20);
        actualPower.add(high);
        actualPower.add(medium);
        actualPower.add(low);
        actualPower.add(critical);
        actualPower.display();
        
        //creat a fuzzy variable for the current behavoir of the saucer
        playStyle = new FuzzyVariable("playStyle", "%", 0, 100, 2);
        FuzzySet safe1 = new FuzzySet("safe 1", 0, 0, 10, 20);
        FuzzySet safe2 = new FuzzySet("safe 2", 0, 10, 20, 30);
        FuzzySet cautious1 = new FuzzySet("caution 1", 10, 20, 30, 40);
        FuzzySet cautious2 = new FuzzySet("caution 2", 20, 30, 40, 50);
        FuzzySet cautious3 = new FuzzySet("caution 3", 30, 40, 50, 60);
        FuzzySet cautious4 = new FuzzySet("caution 4", 40, 50, 60, 70);
        FuzzySet cautious5 = new FuzzySet("caution 5", 50, 60, 70, 80);
        FuzzySet aggressive1 = new FuzzySet("aggressive 1", 60, 70, 80, 90);
        FuzzySet aggressive2 = new FuzzySet("aggressive 2", 70, 80, 90, 100);
        
        playStyle.add(safe1);
        playStyle.add(safe2);
        playStyle.add(cautious1);
        playStyle.add(cautious2);
        playStyle.add(cautious3);
        playStyle.add(cautious4);
        playStyle.add(cautious5);
        playStyle.add(aggressive1);
        playStyle.add(aggressive2);
        
        playStyle.display();
        
        
        accuracy = new FuzzyVariable("accuracy", "%", 0, 100, 2);
        FuzzySet terrible = new FuzzySet("terrible", 0, 0, 20, 25);
        FuzzySet bad = new FuzzySet("bad", 20, 30, 40, 60);
        FuzzySet good = new FuzzySet("good", 40, 60, 70, 80);
        FuzzySet great = new FuzzySet("great", 75, 80, 100, 100);
        
        accuracy.add(terrible);
        accuracy.add(bad);
        accuracy.add(good);
        accuracy.add(great);
        
        accuracy.display();


           
        
        
                //Rule matrix for Playstyle
        playStyleRules = new MamdaniRuleSet();
        
        FuzzySet[] rpsets = {relativeLow, relativeMedium,relativeHigh};
        FuzzySet[] apsets = {high, medium, low , critical};
        
        
         FuzzySet[][] currentPlaystyle =
        {
            // high   medium     low    , critical
            {cautious3,     cautious1,   safe2 ,safe1}, // relativeLow
            {aggressive1,   cautious4,  cautious3 ,cautious2},  // relativeMedium
            {aggressive2, aggressive1, cautious5 , cautious4}  // relativeHigh
        };
        
        
         playStyleRules.addRuleMatrix(
                relativePower, rpsets,
                actualPower, apsets,
                playStyle, currentPlaystyle);
         

        outputControl = new MamdaniRuleSet();

        
        FuzzySet[] rangeSets = {shortRange, midRange, maxRange};
        FuzzySet[] playSets = {safe1, safe2 , cautious1 ,cautious2 ,cautious3 , cautious4 , cautious5 , aggressive1 , aggressive2};
        FuzzySet[][] outputControlMatrix =
        {
            //safe1,     safe2 ,        cautious1       ,cautious2      ,cautious3      , cautious4     , cautious5     , aggressive1   , aggressive2,    aggressive3
            {lowOutput  , lowOutput    ,regularOutput   ,regularOutput , regularOutput, highOutput     , highOutput     , highOutput     , highOutput}, //shortRange
            {nullOutput  , lowOutput ,  lowOutput     , lowOutput     , lowOutput     , lowOutput     , regularOutput   , regularOutput  , highOutput},//midRange
            {nullOutput , nullOutput ,  lowOutput ,     lowOutput     , lowOutput     , lowOutput     , lowOutput     , lowOutput        , regularOutput}//maxRange
        };
        
        
        
        
        
        outputControl.addRuleMatrix(
                range, rangeSets,
                playStyle, playSets,
                outputPower, outputControlMatrix);
        
        //updateRules();
        
    }
       
         
            
    public void senseSaucers(ArrayList<SensorData> data) throws Exception 
    {
        //updateRules();
        // This is where you get told about enemies
        // save whatever information you need in suitable member variables
        
        // find the closest enemy to target - this will be used later in getTarget()
        if(data.size() > 0)
        {
            double closest = data.get(0).distance;
            target = data.get(0);
            for(SensorData thisData: data)
            {
                opponentHealths.add(thisData.energy);
                if(thisData.distance < closest)
                {
                    target = thisData;
                    closest = thisData.distance;
                }
            }
            
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
    


    public void sensePowerUps(ArrayList<SensorData> data) throws Exception 
    {
        //updateRules();
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

     public void senseBlasts(ArrayList<SensorData> data) throws Exception 
    {
        //updateRules();
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
            
            dodgeBlast = closest < Constants.STARFIELD_WIDTH/10;            
        }
        else
        {
            nearestBlast = null;
            dodgeBlast = false;
        }
    }
     
    public void senseEnergy(double energy) throws Exception
    {
        // This is where you get told what your own energy level is
        // Save this information if you need it
       //updateRules();
       this.energy = energy;
    }
    
    // methods below determine what your saucer does

    // fires at random intervals
    public SensorData getTarget() throws Exception 
    {   
        
        
        if(Math.random() < 0.5)
        {
            return target;
        }
        else
        {
            return null;
        }
        //remeber that the old target data must be dumped when the target is switched
    }
            
    public void updateRules() throws Exception
    {
        getTarget();
        
        opponentBearing=target.heading;
        
        playStyleRules.clearVariables();
        outputControl.clearVariables();
        
        // Set fuzzy input variable values
        
        range.setValue(target.distance);
        actualPower.setValue(energy/Constants.SAUCER_START_ENERGY*100);
        relativePower.setValue(energy/(target.energy+1)*100);        
        
        //Check if evasive Maneuveres need to be taken
        
        // fire rules to compute power
        newValue = energy;
        

        
        oldValue=newValue;
        
        playStyleRules.update();
        outputControl.update();
    }
            
    private void createQueue ()
    {
            // Adds elements {0, 1, 2, 3, 4} to queue 
        for (int i=0; i<5; i++)
        {

        }
    
    }
    
    public boolean targetShieldsUp()
    {
        return false;
    }
    
    private boolean evade()
    {
        //if there is activity from the other saucers, then dreanaught must evade
        //must stack
        return false;
    }
    
    private void updateEnemyFirelist()
    {
        // if an enemy deterioration goes between 50 to 100 (nearest), it must be put into a queue
    }
    
    private void updateFireList()
    {
       //The shots fired by dreadnaught must be recorded for accuracy readings
        
        // or scratch that, we'll update her firing control based on relative detirioration and relative power
    }
    
    private void dumpTargetData()
    {
        // this method is used to remove the old targets data if the target is switched
    }
    
    private void findAssialant(double hitfactor)
    {
        //if Dreadnaught is hit it will perform a search looking through 
        // the enemy firelists for fot a likely assailant
    }
            
    private void firingControl() throws Exception
    {
        updateRules();
        final double fullRange = Math.sqrt(Constants.STARFIELD_WIDTH*Constants.STARFIELD_WIDTH+
                    Constants.STARFIELD_HEIGHT*Constants.STARFIELD_HEIGHT);
        
        //get the value of the fuzzy variable power, derived from the other fuzzy variables
        //and multiply it to convert it into a usable output
        double powerValue = (outputPower.getValue()*10);
        
        //The following is a check to ensure that the saucer does not fire 
        //when the firepower is insufficient to cover the distance to the target
        if (powerValue < ((range.getValue())/fullRange)*10)
        {
            deterioration=oldValue-newValue;
            System.out.println(deterioration);
            firepower = 0;
     
        };
        
        //The final return for the firepower, based of power and range
        updateFireList();
        firepower = powerValue*(range.getValue()/fullRange*100);
    }
    
    public double getFirePower() throws Exception
    {
        firingControl();
        return firepower;
    }
    
    //if (powerValue >7)
        //{
        //    return powerValue*((fullRange-range.getValue())/fullRange*100);
        //}
    
    public double getTurn() throws Exception
    {
        updateRules();
        
        double power = outputPower.getValue();
        double rudderShift =(power+((power)*Math.round(Math.random())*(-2)))/2;
        
              
        // Different equations for different scenarios
        if (power > 7)
        {
            return rudderShift*5;
        }
        
        if (playStyle.getValue() > 70 || relativePower.getValue() > 175)
        {
            rudderShift = opponentBearing;
        }
        
        
        return rudderShift*power/5;
        

    }
    
    public double getSpeed() throws Exception
    {
        updateRules();
        // Check if evasive maneuveres need to be taken
        
        //get the value of the fuzzy variable power, derived from the other fuzzy variables
        
        double power = (outputPower.getValue());
        if (playStyle.getValue() > 70 || relativePower.getValue() > 175)
        {
           return ((Saucer.SAUCER_MAX_SPEED)/5 * power);
        }
        
        
        return ((Saucer.SAUCER_MAX_SPEED)/10 * power);
        
    }
    
        public boolean getShields()
    {
        return false;
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
    
}

