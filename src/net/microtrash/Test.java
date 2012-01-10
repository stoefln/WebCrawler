package net.microtrash;

import jade.core.Agent;

public class Test extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		

	}

	@Override
	protected void setup() {
		System.out.println("Hallo World! my name is "+getAID().getName());
		super.setup();
	}
	
}
