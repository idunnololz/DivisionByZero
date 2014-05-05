package com.ggstudios.divisionbyzero;

public class Player {
	private int lives = 0;
	private int gold = 0;
	
	private int kills = 0;
	private int moneyEarned = 0;
	
	public interface OnLivesChangedListener {
		public void onLivesChanged(int lives);
	}
	
	public interface OnGoldChangedListener {
		public void onGoldChanged(int gold);
	}
	
	private boolean listenerSet = false;
	private OnLivesChangedListener livesListener;
	private OnGoldChangedListener goldListener;
	
	public Player(int startingLives, int startingGold) {
		lives = startingLives;
		gold = startingGold;
	}
	
	public void setLives(int lives){
		if(this.lives != lives) {
			this.lives = lives;
			if(listenerSet) {
				livesListener.onLivesChanged(lives);
			}
		}
	}
	
	public void setGold(int gold) {
		if(this.gold != gold) {
			this.gold = gold;
			if(listenerSet) {
				goldListener.onGoldChanged(gold);
			}
		}
	}
	
	public void setListeners(OnLivesChangedListener livesListener,
			OnGoldChangedListener goldListener) {
		this.livesListener = livesListener;
		this.goldListener = goldListener;
		
		livesListener.onLivesChanged(lives);
		goldListener.onGoldChanged(gold);
		
		listenerSet = true;
	}
	
	public void decrementLives() {
		lives--;
		
		if(listenerSet) {
			livesListener.onLivesChanged(lives);
		}
	}

	public void deductGold(int cost) {
		gold -= cost;
		
		if(cost != 0 && listenerSet) {
			goldListener.onGoldChanged(gold);
		}
	}

	public int getGold() {
		return gold;
	}

	public void awardGold(int amount) {
		this.gold += amount;
		
		moneyEarned += amount;
		
		if(amount != 0 && listenerSet) {
			goldListener.onGoldChanged(gold);
		}
	}
	
	public void incrementKill() {
		kills++;
	}
	
	public PlayerSnapshot getSnapshot() {
		PlayerSnapshot snapshot = new PlayerSnapshot();
		snapshot.kills = kills;
		snapshot.lives = lives;
		snapshot.moneyEarned = moneyEarned;
		snapshot.gold = gold;
		return snapshot;
	}
	
	public void loadFromSnapshot(PlayerSnapshot snapshot) {
		kills = snapshot.kills;
		lives = snapshot.lives;
		moneyEarned = snapshot.moneyEarned;
		gold = snapshot.gold;
		
		if(listenerSet) {
			goldListener.onGoldChanged(gold);
			livesListener.onLivesChanged(lives);
		}
	}
	
	public static class PlayerSnapshot {
		int gold, lives, kills, moneyEarned;
	}
}
