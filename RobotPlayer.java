package template;

import battlecode.common.*;

import java.util.*;

public class RobotPlayer {
	static Random rand;
	static Direction facing;

	public static void run(RobotController rc) {
        BaseBot myself;
        rand = new Random(rc.getID());
        facing=getRandomDirection();
        
        if (rc.getType() == RobotType.HQ) {
            myself = new HQ(rc);
        } else if (rc.getType() == RobotType.BEAVER) {
            myself = new Beaver(rc);
        } else if (rc.getType() == RobotType.MINER) {
            myself = new Miner(rc);
        } else if (rc.getType() == RobotType.MINERFACTORY) {
            myself = new MinerFactory(rc);
        } else if (rc.getType() == RobotType.TANKFACTORY) {
            myself = new TankFactory(rc);
        } else if (rc.getType() == RobotType.TANK) {
            myself = new Tank(rc);
        } else if (rc.getType() == RobotType.HANDWASHSTATION) {
            myself = new HandWashStation(rc);
        } else if (rc.getType() == RobotType.BARRACKS) {
            myself = new Barracks(rc);
        } else if (rc.getType() == RobotType.SOLDIER) {
            myself = new Soldier(rc);
        } else if (rc.getType() == RobotType.TOWER) {
            myself = new Tower(rc);
        } else {
            myself = new BaseBot(rc);
        }

        while (true) {
            try {
                myself.go();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	}

	
	
	
	private static Direction getRandomDirection() {
		return Direction.values()[(int)(rand.nextDouble()*8)];
	}




	public static class BaseBot {
        protected RobotController rc;
        protected MapLocation myHQ, theirHQ;
        protected Team myTeam, theirTeam;

        public BaseBot(RobotController rc) {
            this.rc = rc;
            this.myHQ = rc.senseHQLocation();
            this.theirHQ = rc.senseEnemyHQLocation();
            this.myTeam = rc.getTeam();
            this.theirTeam = this.myTeam.opponent();
        }

        public Direction[] getDirectionsToward(MapLocation dest) {
            Direction toDest = rc.getLocation().directionTo(dest);
            Direction[] dirs = {toDest,
		    		toDest.rotateLeft(), toDest.rotateRight(),
				toDest.rotateLeft().rotateLeft(), toDest.rotateRight().rotateRight()};

            return dirs;
        }

        public Direction getMoveDir(MapLocation dest) {
            Direction[] dirs = getDirectionsToward(dest);
            for (Direction d : dirs) {
                if (rc.canMove(d)) {
                    return d;
                }
            }
            return null;
        }

        public Direction getSpawnDirection(RobotType type) {
            Direction[] dirs = getDirectionsToward(this.theirHQ);
            for (Direction d : dirs) {
                if (rc.canSpawn(d, type)) {
                    return d;
                }
            }
            return null;
        }

        public Direction getBuildDirection(RobotType type) {
            Direction[] dirs = getDirectionsToward(this.theirHQ);
            for (Direction d : dirs) {
                if (rc.canBuild(d, type)) {
                    return d;
                }
            }
            return null;
        }

        public RobotInfo[] getAllies() {
            RobotInfo[] allies = rc.senseNearbyRobots(Integer.MAX_VALUE, myTeam);
            return allies;
        }

        public RobotInfo[] getEnemiesInAttackingRange() {
            RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.SOLDIER.attackRadiusSquared, theirTeam);
            return enemies;
        }

        public void attackLeastHealthEnemy(RobotInfo[] enemies) throws GameActionException {
            if (enemies.length == 0) {
                return;
            }

            double minEnergon = Double.MAX_VALUE;
            MapLocation toAttack = null;
            for (RobotInfo info : enemies) {
                if (info.health < minEnergon) {
                    toAttack = info.location;
                    minEnergon = info.health;
                }
            }

            rc.attackLocation(toAttack);
        }

        
        public void beginningOfTurn() {
            if (rc.senseEnemyHQLocation() != null) {
                this.theirHQ = rc.senseEnemyHQLocation();
            }
        }

        public void endOfTurn() {
        }

        public void go() throws GameActionException {
            beginningOfTurn();
            execute();
            endOfTurn();
        }

        public void execute() throws GameActionException {
            rc.yield();
        }

    	
		public void spawnUnit(RobotType type) throws GameActionException {
			if(rc.getTeamOre()>type.oreCost){
				Direction dir = this.getSpawnDirection(type);
				if(rc.isCoreReady() && rc.canSpawn(dir,type)){
					rc.spawn(dir,type);
				} 
			}
		}
		
		private void buildUnit(RobotType type) throws GameActionException {
			if(rc.getTeamOre()>type.oreCost){
				Direction buildDir = this.getBuildDirection(type);
				if(rc.isCoreReady()&&rc.canBuild(buildDir,type)){
					rc.build(buildDir,type);
				}
			}

		}

		private void transferSupplies() throws GameActionException {
			RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,rc.getTeam());
			double lowestSupply = rc.getSupplyLevel();
			double transferAmount = 0;
			MapLocation suppliesToThisLocation = null;
			for (RobotInfo ri:nearbyAllies){
				if (ri.supplyLevel < lowestSupply){
					lowestSupply = ri.supplyLevel;
					transferAmount = (rc.getSupplyLevel()-ri.supplyLevel)/2.0;
					suppliesToThisLocation = ri.location;
				}
			}
			if (suppliesToThisLocation != null){
				rc.transferSupplies((int)transferAmount,suppliesToThisLocation);
			}
		}
		
		private void mineAndMove() throws GameActionException {
			if(rc.senseOre(rc.getLocation())>1){
				if(rc.isCoreReady()&&rc.canMine()){
					rc.mine();
				}
			}else{//no ore nearby
				moveAround();
			}
		}




		private void moveAround() throws GameActionException {
			if(rand.nextDouble()<0.05){
				if(rand.nextDouble()<.5){
					facing=facing.rotateLeft();
				}else{
					facing=facing.rotateRight();
				}

			}
			MapLocation tileInFront = rc.getLocation().add(facing);

			//check that location in front is not a location that can be attacked by enemy towers
			MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
			boolean tileInFrontSafe = true;
			for(MapLocation m:enemyTowers){
				if( m.distanceSquaredTo(tileInFront)<=RobotType.TOWER.attackRadiusSquared){
					tileInFrontSafe =false;
					break;
				}
			}
			//check we're not facing off the edge of the map
			if(rc.senseTerrainTile(tileInFront)!=TerrainTile.NORMAL||!tileInFrontSafe){
				facing = facing.rotateLeft();
			}else if(rc.isCoreReady()&&rc.canMove(facing)){ //try to move in th	e facing direction
				rc.move(facing);
			}

		}
    }

    
    
    
    
    
    
    
    
    public static class HQ extends BaseBot {
        public HQ(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            super.attackLeastHealthEnemy(this.getEnemiesInAttackingRange());
        	//if before round ~500?
            super.spawnUnit(RobotType.BEAVER);
        	rc.yield();
        }
    }

    public static class Beaver extends BaseBot {
        public Beaver(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }

    public static class Miner extends BaseBot {
        public Miner(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }
    
    public static class MinerFactory extends BaseBot {
        public MinerFactory(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }
    
    public static class TankFactory extends BaseBot {
        public TankFactory(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }
    
    public static class Tank extends BaseBot {
        public Tank(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }
    
    public static class HandWashStation extends BaseBot {
        public HandWashStation(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }
    
    public static class Barracks extends BaseBot {
        public Barracks(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }

    public static class Soldier extends BaseBot {
        public Soldier(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }

    public static class Tower extends BaseBot {
        public Tower(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }
}