maps=DefaultHuge,DefaultLarge,DefaultMedium,DefaultSmall,Fossil,Gears,Justice,Mirage,Money,MoneyTower,Racetrack,Restart,SMILE,SaltyPepper,TargetPractice,Thirds,UglySweater,UnderTheSea
#maps=DefaultHuge,DefaultLarge,DefaultMedium,DefaultSmall,Fossil,Gears,Justice,SMILE,TargetPractice

#maps=Arkanoid,Crossed,Cubic,DefaultHuge,DefaultLarge,DefaultMedium,DefaultSmall,Dispensers,Escape,Fossil,Gears,GoofyMap,Jail,Justice,Lattice,Mirage,Money,MoneyTower,MountainRange,Narrow,QuestionableFlag,Racetrack,Restart,RusherNightmare,SMILE,SaltyPepper,Spiral,TargetPractice,TheMines,Thirds,UglySweater,UnderTheSea,Understand,Versus,badrush,catface,gardenworld,memstore
gradlew run -Pmaps=$maps -PteamA=$2 -PteamB=$1
gradlew run -Pmaps=$maps -PteamA=$1 -PteamB=$2
