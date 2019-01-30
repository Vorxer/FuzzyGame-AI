package battle.saucers.controllers;


/*

Dreadnought V 1.1.0 

      --Notes--
This version ported directly from Dreadnought MK1
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




public class Dreadnought implements SaucerController
{
    // This will be the rule set
    
    
    private MamdaniRuleSet confidenceRules;
    private MamdaniRuleSet firePowerRules;
    
    
    // These will be the input variables
    
    private FuzzyVariable relativePower; // The power of the saucer in comparison to the opponent
    private FuzzyVariable averagePower;
    private FuzzyVariable confidence;// The power of the saucer as a percentage of total powe
    private FuzzyVariable range;         // The distance from the opponent
    private FuzzyVariable playStyle;
    private FuzzyVariable firePower;
    FuzzySet[] rpsets;
    FuzzySet[] apsets;
    FuzzySet[] currentPlaystyle;
    
    private static final Random random = new Random();
    private static final String NAME = "Dreadnought";
    private static final Color BASE = Color.gray;
    private static final Color ARROW = Color.darkGray;
    public double opponentBearing;
    
    
    private SensorData target;
    private SensorData originalTarget;
    private double nearestPowerUpDirection;
    private boolean powerUpOn;
    private SensorData nearestBlast;
    private boolean dodgeBlast = false;
    private double energy;
    private double accuracy;
    private double localDeterioration;
    private double targetDeterioration;
    private double newValue;
    private double oldValue;
    private double averageHealth;
    private double damageOverPeriod;
    private SensorData closestTarget;
    private double targetDamageOverPeriod;
    private double localInitDmg=10000;
    private double targetInitDmg=10000;
    private double closestBlastProximity;
    private SensorData closestBlastData;
    private double expectedDamageOverPeriod;
    private double doubleFirePower;
    private double speed = 0;
    private double heading=0;
    private ArrayList<SensorData> opponentData = new ArrayList<SensorData>();
    private ArrayList<Double> blankArray= new ArrayList<Double>();
    private ArrayList<Double> opponentHealths = new ArrayList<Double>();
    private ArrayList<Double> fireTracker = new ArrayList<Double>();
    private ArrayList<Double> localDamageSense = new ArrayList<Double>();
    private ArrayList<Double> targetDamageSense = new ArrayList<Double>();
    
    
    
    
    
    
    public Dreadnought() throws FuzzyException, Exception
    {
        double initHealth=10000;
        localInitDmg=initHealth;
        targetInitDmg=initHealth;
        targetDamageOverPeriod=0;
        expectedDamageOverPeriod=0;
        
        double zero=0;
        for(int i = 0; i < 50; ++i)blankArray.add(zero);

        
        //create a variable for range, which will be used to put the distance from the target into perspective


         
        

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
          
        
        firePower = new FuzzyVariable("accuracy", "%", 0, 100, 2);
        FuzzySet low = new FuzzySet("terrible", 0, 0, 12.5, 25);
        FuzzySet mid1 = new FuzzySet("bad", 0, 12.5, 25, 37.5);
        FuzzySet mid2= new FuzzySet("good", 25, 37.5, 50, 62.5);
        FuzzySet high = new FuzzySet("great", 62.5, 75, 100, 100);
        
        firePower.add(low);
        firePower.add(mid1);
        firePower.add(mid2);
        firePower.add(high);  
        firePower.display();
        
        final double fullRange = 750;
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
        
        FuzzySet[] firepowerYSet = {confidenceLow,confidenceMedium,confidenceHigh,confidenceVeryHigh};
        FuzzySet[] firepowerXSet = {shortRange, midRange , maxRange};
        FuzzySet[][] firePowerRulesMatrix =
        {
            //{confidenceLow,confidenceMedium,confidenceHigh,confidenceVeryHigh};
            {mid2,mid2,high,high}, //shortRange
            {mid1,mid2,mid2,high}, //midRange
            {low,mid1,mid2,high}, //maxRange
        };
        
                
        
        firePowerRules = new MamdaniRuleSet();
        
        confidenceRules.addRuleMatrix(
               
                range, firepowerXSet,
                 confidence, firepowerYSet,
                firePower, firePowerRulesMatrix);
    
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


            double sum = 0;
            for(Double d : localDamageSense)sum += d;

            damageOverPeriod = sum;

        } 
    }
    
    public void updateOpponentDamageSensors()
    {
        try{
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
                //localDamageSense.remove(0);   
            }
            targetInitDmg=target.energy;

 

        double sum = 0;
        for(Double d : targetDamageSense)sum += d;
        targetDamageOverPeriod = sum;


        }
        }
        
        catch(NullPointerException e){
            
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

  
        
    double sum = 0;
    for(Double d : fireTracker)sum += d;
    expectedDamageOverPeriod = sum;

        
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
            closestTarget = opponentData.get(0);
            for(SensorData thisData: opponentData)
            {
                opponentHealths.add(thisData.energy);
                if(thisData.distance < closest)
                {
                    closestTarget = thisData;
                    closest = thisData.distance;
                    
                }
            }
            
            averageHealth=reqAveragePower(opponentData);

            
            opponentHealths.clear();
                    
        updateLocalSensors();
        updateOpponentDamageSensors();
        accuracy=targetDamageOverPeriod/(expectedDamageOverPeriod+1);

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
        if(data.size() > 0)
        {
            closest = data.get(0).distance;
            closestBlastProximity=closest;
            nearestBlast = data.get(0);
            
            for(SensorData thisData: data)
            {
                if(thisData.distance < closest)
                {
                    nearestBlast = thisData;
                    closest = thisData.distance;
                    closestBlastProximity=closest;
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
 
            targetingSystem();
            return target;
     //remeber that the old target data must be dumped when the target is switched
    }
            
    public void updateRules() throws Exception
    {
        
        
        
        getTarget();
        try{
        opponentBearing=target.heading;
        }
        catch(NullPointerException e)
        {
            opponentBearing=0;
        }
        confidenceRules.clearVariables();
        range.setValue(750);
        // Set fuzzy input variable values
        if (target.distance<750){
        
        range.setValue(target.distance);
        
        }
        averagePower.setValue(energy/(reqAveragePower(opponentData)+1)*100);
        try{
        if (opponentData.size() > 0)
        {
        relativePower.setValue(energy/(reqStrongestOpponent(opponentData).energy+1)*100);        
        }
        }
        catch(NullPointerException e){
        averagePower.setValue(energy);
        }
        //Check if evasive Maneuveres need to be taken
        
        // fire rules to compute power
        newValue = energy;
        

        
        oldValue=newValue;
        try{
        confidenceRules.update();
        }
        catch(FuzzyVariableNotSetException e){
            
        }
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
        // this method is used to remove the old targets data if the target is switched
    }
    
            
    private void firingControl() throws Exception
    {
        updateRules();
        final double fullRange = Math.sqrt(Constants.STARFIELD_WIDTH*Constants.STARFIELD_WIDTH+
                    Constants.STARFIELD_HEIGHT*Constants.STARFIELD_HEIGHT);
        
        //get the value of the fuzzy variable power, derived from the other fuzzy variables
        //and multiply it to convert it into a usable output
        double powerValue;
        try{
        powerValue = firePower.getValue();
        }
        catch(FuzzyVariableNotSetException e)
        {
            powerValue=0;
        }
        //The following is a check to ensure that the saucer does not fire 
        //when the firepower is insufficient to cover the distance to the target
        doubleFirePower=powerValue;
        
        if (powerValue < ((range.getValue())/fullRange)*100)
        {
            updateLocalSensors();

            doubleFirePower = 0;
     
        };

    }
    
    private void targetingSystem()  throws Exception
    {
        target=closestTarget;
        try
        {

        
        if (confidence.getValue() > 1.5)
        {
            target=reqStrongestOpponent(opponentData);
        }
        }
        catch(FuzzyVariableNotSetException e){}
        
        try
        {
        if (target.ID!=originalTarget.ID)
        {
            resetTargetData();
        }
        }
        catch(NullPointerException e){}
        
        originalTarget=target;

    }
    

    private void throttleBoard(double originalSpeed) throws Exception
    {
        try{
        speed = 50*confidence.getValue();
        }
        catch(FuzzyVariableNotSetException e)
        {
            speed = 50;
        }
        if (closestBlastProximity < 500)
        {
            speed=nearestBlast.energy;

        }
        
        if (closestBlastProximity < 350)
        {
            
            //else
            {
                speed=nearestBlast.energy+25;
            }
        }
        
        if (getShields()==true)
        {
            speed = 0;
        }
        
        if ((powerUpOn==true))
        {
            speed=Constants.SAUCER_MAX_POWER;
        }
        
    }
    private void navigation() throws Exception
    {
        heading =0;
        if (accuracy>50)
        {
            heading = target.direction;
        }
        if (opponentData.size()>4 && confidence.getValue()<1.2)
        {
            heading = closestTarget.direction+180;
            speed = 50;
        }
        if (energy<averageHealth)
        {
            heading = closestTarget.direction+180;
            speed = 50;
        }
    }
    private void steeringBoard(double originalHeading) throws Exception
    {
        //heading = originalHeading+confidence.getValue();
        
        
        if (closestTarget.distance < 300)
        {
            heading = 3;
        }
        if (closestBlastProximity < 400)
        {
            //if (originalHeading>50)
            //{
                heading = nearestBlast.direction+45;
            //}
            
            //else
            //{
            //    heading = heading+(20*confidence.getValue());
            //}
        }
        
        if (powerUpOn==true)
        {
            heading=nearestPowerUpDirection;
        }
                
        if (closestBlastProximity < 300)
        {
               heading=nearestBlast.direction+90;
        }
        

        
    }
    
    
    public double getFirePower() throws Exception
    {       
        firingControl();
        upadteDamageOutputSensors(doubleFirePower);
        //return firepower;
        if (target.distance>750)
        { 
            return 0;
        }
        return doubleFirePower;
    }
    
    //if (powerValue >7)
        //{
        //    return powerValue*((fullRange-range.getValue())/fullRange*100);
        //}
    
    public double getTurn() throws Exception
    {
        updateRules();
        navigation();
        steeringBoard(heading);
 
        return heading;
        

    }
    
    public double getSpeed() throws Exception
    {
        updateRules();
        navigation();
        throttleBoard(speed);

        return speed;
        
        
    }
    
    public boolean getShields()
    {

        try{
        
        if (closestBlastProximity<60)
        {
            return true;
        }
        }
        catch(NullPointerException e)
        {
            
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
        try{
            double strongest = data.get(0).energy;
            SensorData strongestSaucer = data.get(0);
            for(SensorData thisData: data)
            {
                if(thisData.energy > strongest)
                {
                    strongestSaucer = thisData;
                    strongest = thisData.energy;
                    return strongestSaucer;  
                }
            }
        }
        catch(NullPointerException  e)
        {}
        return data.get(0);
              
    }
   
    
    
}

