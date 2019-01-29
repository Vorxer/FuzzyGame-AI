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
    
    
    private MamdaniRuleSet confidenceRules;
    
    
    // These will be the input variables
    
    private FuzzyVariable relativePower; // The power of the saucer in comparison to the opponent
    private FuzzyVariable averagePower;
    private FuzzyVariable confidence;// The power of the saucer as a percentage of total power
    private FuzzyVariable playStyle;     // The variable for style of play
    private FuzzyVariable range;         // The distance from the opponent
    private FuzzyVariable accuracy;
    
    FuzzySet[] rpsets;
    FuzzySet[] apsets;
    FuzzySet[] currentPlaystyle;
    
    private static final Random random = new Random();
    private static final String NAME = "Enterprise";
    private static final Color BASE = Color.gray;
    private static final Color ARROW = Color.darkGray;
    public double opponentBearing;
    
    
    private SensorData target;
    private double nearestPowerUpDirection;
    private boolean powerUpOn;
    private SensorData nearestBlast;
    private boolean dodgeBlast = false;
    private double energy;
    private double localDeterioration;
    private double targetDeterioration;
    private double newValue;
    private double oldValue;
    private double firepower;
    private double averageHealth;
    private double damageOverPeriod;
    private double targetDamageOverPeriod;
    private double localInitDmg=10000;
    private double targetInitDmg=10000;
    private double closestBlastProximity;
    private double expectedDamageOverPeriod;
    private double speed = 0;
    private double heading=0;
    private ArrayList<SensorData> opponentData = new ArrayList<SensorData>();
    private ArrayList<Double> blankArray= new ArrayList<Double>();
    private ArrayList<Double> opponentHealths = new ArrayList<Double>();
    private ArrayList<Double> fireTracker = new ArrayList<Double>();
    private ArrayList<Double> localDamageSense = new ArrayList<Double>();
    private ArrayList<Double> targetDamageSense = new ArrayList<Double>();
    
    
    
    
    
    
    public Enterprise() throws FuzzyException, Exception
    {
        double initHealth=10000;
        localInitDmg=initHealth;
        targetInitDmg=initHealth;
        targetDamageOverPeriod=0;
        expectedDamageOverPeriod=0;
        
        double zero=0;
        for(int i = 0; i < 50; ++i)blankArray.add(zero);
        //opponentHealths=blankArray;
        //localDamageSense=blankArray;
        //targetDamageSense=blankArray;
        //System.out.println(localDamageSense.size());
        //System.out.println(localDamageSense);
        
        
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
         
        

        // create fuzzy variable relative power between the Saucer and the opponent as a percentage
        double maxpower = 1000000000;
        //double maxpower = 200;
        FuzzySet powerLow = new FuzzySet("Low", 0, 0, 40, 50);
        FuzzySet powerMidLow = new FuzzySet("Mid Low", 40, 50, 60, 70);
        FuzzySet powerMedium = new FuzzySet("Medium", 60, 70, 110, 130);
        FuzzySet powerHigh = new FuzzySet("High", 110, 130, 150, 175);
        FuzzySet powerVeryHigh = new FuzzySet("Very High", 150, 175, maxpower, maxpower);
        
        relativePower = new FuzzyVariable("relativepower", "%", 0, maxpower, 2);
        relativePower.add(powerLow);
        relativePower.add(powerMidLow);
        relativePower.add(powerMedium);
        relativePower.add(powerHigh);
        relativePower.add(powerVeryHigh);
        relativePower.display();     
        
        averagePower = new FuzzyVariable("averagepower", "%", 0, maxpower, 2);
        averagePower.add(powerLow);
        averagePower.add(powerMidLow);
        averagePower.add(powerMedium);
        averagePower.add(powerHigh);
        averagePower.add(powerVeryHigh);
        averagePower.display();
        
        confidence = new FuzzyVariable("confidence", "%", 0.3, 2, 2);
        FuzzySet confidenceLow = new FuzzySet("Low", 0.3, 0.4, 0.5, 0.6 );
        FuzzySet confidenceMedium = new FuzzySet("Mid Low", 0.5, 0.6, 1, 1.25);
        FuzzySet confidenceHigh = new FuzzySet("Medium", 1, 1.25, 1.5, 1.75);
        FuzzySet confidenceVeryHigh = new FuzzySet("High", 1.5, 1.75, 2, 2);
        
        confidence.add(confidenceLow);
        confidence.add(confidenceMedium);
        confidence.add(confidenceHigh);
        confidence.add(confidenceVeryHigh);
        confidence.display();
        
        
        confidenceRules = new MamdaniRuleSet();

        
        FuzzySet[] yset = {powerLow, powerMidLow, powerMedium,powerHigh,powerVeryHigh};
        FuzzySet[] xset = yset; //{safe1, safe2 , cautious1 ,cautious2 ,cautious3 , cautious4 , cautious5 , aggressive1 , aggressive2};
        FuzzySet[][] confidenceRulesMatrix =
        {
            //av powerLow ,powerMidLow   powerMedium   powerHigh     powerVeryHigh       relative
            {confidenceLow,confidenceLow,confidenceLow,confidenceMedium,confidenceHigh}, //powerLow
            {confidenceLow,confidenceLow,confidenceMedium,confidenceMedium,confidenceHigh}, //powerMidLow
            {confidenceLow,confidenceLow,confidenceMedium,confidenceMedium,confidenceHigh}, //powerMedium
            {confidenceLow,confidenceMedium,confidenceHigh,confidenceHigh,confidenceVeryHigh}, //powerHigh
            {confidenceLow,confidenceMedium,confidenceHigh,confidenceVeryHigh,confidenceVeryHigh} //powerVeryHigh
        };
        
        
        
        
        
        confidenceRules.addRuleMatrix(
                relativePower, yset,
                averagePower, xset,
                confidence, confidenceRulesMatrix);
          
        
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
    
    }
    
    public void updateLocalSensors()
    {
        double damagetaken = localInitDmg - energy;
        
        if (damagetaken < 0)
        {
            
            int count = 0 ;
            for (double i: localDamageSense){
                 localDamageSense.set(count,i+damagetaken);
                 count++;
            }
        }
        
        else
        {

            if (damagetaken<10)
            {
                localDeterioration = damagetaken;
            }
                
            if (damagetaken>10)
             {
                localDamageSense.add(damagetaken);
                if (localDamageSense.size()>10)
                    {
                        localDamageSense.remove(0);
                    }

                    //localDamageSense.remove(0);   
                }
            localInitDmg=energy;


            //System.out.println("oppoent "+targetDeterioration);
            //System.out.println(localDamageSense);    

            double sum = 0;
            for(Double d : localDamageSense)sum += d;

            damageOverPeriod = sum;
            //System.out.println(damageOverPeriod);
        } 
    }
    
    public void updateOpponentDamageSensors()
    {
        double damagetaken = targetInitDmg - target.energy;
        
        if (damagetaken < 0)
        {
            
            int count = 0 ;
            for (double i: targetDamageSense){
                 targetDamageSense.set(count,i+damagetaken);
                 count++;
            }
        }
        
        else
        {

            if (damagetaken<10)
            {
                targetDeterioration = damagetaken;
            }
            if (damagetaken>10)
            {
                targetDamageSense.add(damagetaken);
                if (targetDamageSense.size()>10)
                {
                    targetDamageSense.remove(0);
                }
                //System.out.println(targetDamageSense); 
                //localDamageSense.remove(0);   
            }
            targetInitDmg=target.energy;


        //System.out.println("oppoent "+targetDeterioration);
        //System.out.println(localDamageSense);    

        double sum = 0;
        for(Double d : targetDamageSense)sum += d;
        targetDamageOverPeriod = sum;

        //System.out.println("targetD"+targetDeterioration);
        //System.out.println("targetDPM"+targetDamageOverPeriod);
        }
        
    }
    
    public void upadteDamageOutputSensors(double damage)
    {
        
        double damagetaken = targetInitDmg - target.energy;
 
        if (damagetaken<0)
        {
            fireTracker.add(damage*Constants.SAUCER_HIT_FACTOR);
        }
        
        if (fireTracker.size()>10)
        {
            fireTracker.remove(0);
        }

        
        
    //System.out.println("oppoent "+targetDeterioration);
    //System.out.println(localDamageSense);    
        
    double sum = 0;
    for(Double d : fireTracker)sum += d;
    expectedDamageOverPeriod = sum;
    
    //System.out.println("targetD"+targetDeterioration);
    //System.out.println("targetDPM"+targetDamageOverPeriod);
        
    }
    

         
            
    public void senseSaucers(ArrayList<SensorData> data) throws Exception 
    {
        //updateRules();
        // This is where you get told about enemies
        // save whatever information you need in suitable member variables
        // find the closest enemy to target - this will be used later in getTarget()
        opponentData=data;
        
        if(opponentData.size() > 0)
        {
            double closest = opponentData.get(0).distance;
            target = opponentData.get(0);
            for(SensorData thisData: opponentData)
            {
                opponentHealths.add(thisData.energy);
                if(thisData.distance < closest)
                {
                    target = thisData;
                    closest = thisData.distance;
                }
            }
            
            averageHealth=reqAveragePower(opponentData);
            //System.out.println("Average Health = "+averageHealth);
            //System.out.println("Health of Strongest = "+reqStrongestOpponent(opponentData));
            //System.out.println("So-OH = "+opponentHealths.size());
            //System.out.println("local "+localDeterioration);
            //System.out.println("oppoent "+targetDeterioration);
            //System.out.println("count "+localDamageSense);
            //System.out.println("damagecounter "+damageOverPeriod);
            
            opponentHealths.clear();
                    
        updateLocalSensors();
        updateOpponentDamageSensors();
        System.out.println("accuracy=" + targetDamageOverPeriod/expectedDamageOverPeriod);
        //accuracy.setValue(targetDamageOverPeriod/expectedDamageOverPeriod);
        //System.out.println("accuracy=" + accuracy.getValue());
        }
        else
        {
            opponentData = null;
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
        double closest=1000;
        closestBlastProximity=closest;
        System.out.println("size= "+data.size());
        if(data.size() > 0)
        {
            closest = data.get(0).distance;
            closestBlastProximity=closest;
            nearestBlast = data.get(0);
            
            for(SensorData thisData: data)
            {
                System.out.println("actual "+thisData.distance);
                System.out.println("got here");
                if(thisData.distance < closest)
                {
                    nearestBlast = thisData;
                    closest = thisData.distance;
                    closestBlastProximity=closest;
                    System.out.println(thisData.distance);
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
        
        

            return reqStrongestOpponent(opponentData);

        //remeber that the old target data must be dumped when the target is switched
    }
            
    public void updateRules() throws Exception
    {
        
        
        getTarget();
        
        opponentBearing=target.heading;
        confidenceRules.clearVariables();
        
        // Set fuzzy input variable values
        
        range.setValue(target.distance);
        averagePower.setValue(energy/(reqAveragePower(opponentData)+1)*100);
        
        if (opponentData.size() > 0)
        {
        relativePower.setValue(energy/(reqStrongestOpponent(opponentData).energy+1)*100);        
        }
        //Check if evasive Maneuveres need to be taken
        
        // fire rules to compute power
        newValue = energy;
        

        
        oldValue=newValue;
        
        confidenceRules.update();
    }
    
    

    
    private boolean opponentIsViable()
    {
        return true;
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
    
    
    private void resetTargetData()
    {
        expectedDamageOverPeriod = 0;
        targetDamageOverPeriod = 0;
        targetDeterioration = 0;
        targetInitDmg=target.energy;
        targetDamageSense.clear();
        fireTracker.clear();
        System.out.println("Target Data Reset");
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
        double powerValue = (50*confidence.getValue());
        
        //The following is a check to ensure that the saucer does not fire 
        //when the firepower is insufficient to cover the distance to the target
        firepower=powerValue;
        
        if (powerValue < ((range.getValue())/fullRange)*10)
        {
            updateLocalSensors();
            //System.out.println(deterioration);
            firepower = 0;
     
        };
        
        
        
        
        
        //The final return for the firepower, based of power and range
        //firepower = powerValue*(range.getValue()/fullRange*100);
    }
    
    private void targetingSystem()
    { //if (confidence is high)
        
    }
    
    private void throttleBoard(double originalSpeed) throws Exception
    {
        speed = 50*confidence.getValue();
        
        if (closestBlastProximity < 400)
        {
            if (originalSpeed>50)
            {
                speed = speed-(20*confidence.getValue());
            }
            
            else
            {
                speed = speed+(20*confidence.getValue());
            }
        }
        
        if (closestBlastProximity < 250)
        {
            if (originalSpeed>50)
            {
                speed = 0;
            }
            
            else
            {
                speed=125;
            }
        }
        
        if (getShields()==true)
        {
            speed = 0;
        }
        
        if (powerUpOn==true)
        {
            speed=125;
        }
        
    }
    
    private void steeringBoard(double originalHeading) throws Exception
    {
        //heading = originalHeading+confidence.getValue();
        heading =0;
        
        if (closestBlastProximity < 400)
        {
            //if (originalHeading>50)
            //{
                heading = heading-(20*confidence.getValue());
            //}
            
            //else
            //{
            //    heading = heading+(20*confidence.getValue());
            //}
        }
        
        if (closestBlastProximity < 250)
        {
               heading=originalHeading-45;
        }
        
        if (powerUpOn==true)
        {
            heading=nearestPowerUpDirection;
        }
        
    }
    
    
    public double getFirePower() throws Exception
    {       
        firingControl();
        upadteDamageOutputSensors(firepower);
        return firepower;
    }
    
    //if (powerValue >7)
        //{
        //    return powerValue*((fullRange-range.getValue())/fullRange*100);
        //}
    
    public double getTurn() throws Exception
    {
        updateRules();
        steeringBoard(heading);
 
        return heading;
        

    }
    
    public double getSpeed() throws Exception
    {
        updateRules();
        throttleBoard(speed);
        System.out.println("confidence = "+confidence.getValue());
        System.out.println("speed = "+speed);
        return speed;
        
        
    }
    
    public boolean getShields()
    {
        System.out.println(closestBlastProximity);
        if (closestBlastProximity<80 && powerUpOn==false)
        {
            return true;
        }
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
    
    private double reqAveragePower(ArrayList<SensorData> data) 
    {
    double sum = 0;
    if(!data.isEmpty()) {
      for (SensorData mark : data) {
          sum += mark.energy;
      }
      return sum/ data.size();
    }
    return sum;
    }
    
    public SensorData reqStrongestOpponent(ArrayList<SensorData> data)
    {
            double strongest = opponentData.get(0).energy;
            SensorData strongestSaucer = opponentData.get(0);
            for(SensorData thisData: opponentData)
            {
                if(thisData.energy > strongest)
                {
                    strongestSaucer = thisData;
                    strongest = thisData.energy;
                }
            }        
            return strongestSaucer;    
    }
    
    public SensorData reqWeakesttOpponent(ArrayList<SensorData> data)
    {
            double weakest = opponentData.get(0).energy;
            SensorData weakestSaucer = opponentData.get(0);
            for(SensorData thisData: opponentData)
            {
                if(thisData.energy < weakest)
                {
                    weakestSaucer = thisData;
                    weakest = thisData.energy;
                }
            }        
            return weakestSaucer;    
    }
    
}

