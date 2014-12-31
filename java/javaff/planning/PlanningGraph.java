/************************************************************************
 * Strathclyde Planning Group,
 * Department of Computer and Information Sciences,
 * University of Strathclyde, Glasgow, UK
 * http://planning.cis.strath.ac.uk/
 * 
 * Copyright 2007, Keith Halsey
 * Copyright 2008, Andrew Coles and Amanda Smith
 * Copyright 2011, David Pattison
 *
 * (Questions/bug reports now to be sent to Andrew Coles)
 *
 * This file is part of JavaFF.
 * 
 * JavaFF is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JavaFF is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JavaFF.  If not, see <http://www.gnu.org/licenses/>.
 * 
 ************************************************************************/

package javaff.planning;

import javaff.data.Action;
import javaff.data.CompoundLiteral;
import javaff.data.Fact;
import javaff.data.GroundProblem;
import javaff.data.GroundFact;
import javaff.data.TotalOrderPlan;
import javaff.data.adl.Imply;
import javaff.data.metric.Function;
import javaff.data.metric.NamedFunction;
import javaff.data.strips.And;
import javaff.data.strips.InstantAction;
import javaff.data.strips.Not;
import javaff.data.strips.Proposition;
import javaff.data.strips.STRIPSInstantAction;
import javaff.data.strips.SingleLiteral;
import javaff.data.strips.TrueCondition;
import javaff.data.temporal.DurativeAction;
import javaff.graph.ActionMutex;
import javaff.graph.FactMutex;
import javaff.planning.PlanningGraph.MutexPair;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Hashtable;

public class PlanningGraph
{
	// ******************************************************
	// Data Structures
	// ******************************************************
	protected Map<Fact, PGFact> propositionMap = new Hashtable<Fact, PGFact>(); // (Fact
																				// =>
																				// PGProposition)
	protected Map<Action, PGAction> actionMap = new Hashtable<Action, PGAction>(); // (Action
																				// =>
																				// PGAction)

	protected Set propositions = new HashSet();// cant use Fact generics because,
											// insanely, it is used to store
											// both Propositions and
											// PGPropositions
	protected Set<PGAction> actions = new HashSet<PGAction>();
	protected Set<PGAction> negativePCActions = new HashSet<PGAction>(); //quick lookup for actions which only have negative PCs -- better than looking through all actions at every layer

	protected Set<PGFact> initial, goal;
	protected Set<MutexPair> propMutexes;
	protected Set<MutexPair> actionMutexes;
	// public List memorised; //5/5/12 -- removed this because I dont think its
	// actually doing anything. The real FF does use memorisation, but JavaFF
	// doesnt seem to have this implemented properly

	protected List<Set<Fact>> factLayers = new ArrayList<Set<Fact>>();
	

	protected Set<PGAction> readyActions = null; // PGActions that have all
							
	protected boolean level_off = false;
	protected static int NUMERIC_LIMIT = 4;
	protected int numeric_level_off = 0;
	protected int num_layers;

	// ******************************************************
	// Main methods
	// ******************************************************
	protected PlanningGraph()
	{
		this.actionMap = new HashMap<Action, PlanningGraph.PGAction>();
		this.actionMutexes = new HashSet<MutexPair>();
		this.actions = new HashSet<PGAction>();
		this.factLayers = new ArrayList<Set<Fact>>();
		this.goal = new HashSet<PlanningGraph.PGFact>();
		this.initial = new HashSet<PlanningGraph.PGFact>();
		this.level_off = false;
		this.num_layers = 0;
		this.numeric_level_off = 4;
		this.propMutexes = new HashSet();
		this.propositionMap = new HashMap<Fact, PlanningGraph.PGFact>();
		this.propositions = new HashSet();
		this.readyActions = new HashSet<PlanningGraph.PGAction>();
	}
	
//	/**
//	 * Initialise this planning graph based upon another. Note that this does not actually construct
//	 * the PG, see clone() for that functionality. Instead, it is intended for PGs which have the same
//	 * resources (actions, proposition etc), so that the internals of the PG do not need to be set
//	 * up every time. 
//	 * 
//	 * @param existingGraph
//	 */
//	public PlanningGraph(PlanningGraph existingGraph)
//	{
//		this();
//		
//		this.actionMap = existingGraph.actionMap;
//		this.actionMutexes = existingGraph.actionMutexes;
//		this.actions = existingGraph.actions;
////		this.factLayers = existingGraph.factLayers;
//		this.goal = existingGraph.goal;
//		this.initial = existingGraph.initial;
////		this.level_off = existingGraph.level_off;
////		this.num_layers = existingGraph.num_layers;
////		this.numeric_level_off = existingGraph.numeric_level_off;
//		this.propMutexes = existingGraph.propMutexes;
//		this.propositionMap = existingGraph.propositionMap;
//		this.propositions = existingGraph.propositions;
////		this.readyActions = existingGraph.readyActions;
////		this.state = existingGraph.state;
//	}

	public PlanningGraph(GroundProblem gp)
	{
		this(gp.getActions(), gp.getGoal());
//		this.setInitial(gp.state);
	}

	public PlanningGraph(Set groundActions, GroundFact goal)
	{
		this();

		setActionMap(groundActions);
		setLinks();
		createNoOps();
		setGoal(goal);
	}

	/**
	 * Populate a new plan graph from an existing plan graph. Note that the data within the existing graph
	 * is only references, not cloned/ 
	 * @param existingPG
	 */
	protected PlanningGraph(PlanningGraph existingPG)
	{
		this.actionMap = existingPG.actionMap;

		this.actionMutexes = existingPG.actionMutexes;
		this.actions = existingPG.actions;
		this.factLayers = existingPG.factLayers;
		this.goal = existingPG.goal;


		this.initial = existingPG.initial;
		this.level_off = existingPG.level_off;
		this.num_layers = existingPG.num_layers;
		this.numeric_level_off = existingPG.numeric_level_off;
		this.propMutexes = existingPG.propMutexes;
		this.propositionMap = existingPG.propositionMap;
		this.propositions = existingPG.propositions;
		this.readyActions = existingPG.readyActions;
		this.negativePCActions = existingPG.negativePCActions;
	}

	public Object clone()
	{
		PlanningGraph clone = new PlanningGraph();
		clone.actionMap = new HashMap<Action, PlanningGraph.PGAction>(
				this.actionMap);

		clone.actionMutexes = new HashSet<MutexPair>(this.actionMutexes);
		clone.actions = new HashSet<PGAction>(this.actions);
		clone.factLayers = new ArrayList<Set<Fact>>(this.factLayers);
		clone.goal = new HashSet<PGFact>(this.goal);


		clone.initial = new HashSet<PGFact>(this.initial);
		clone.level_off = this.level_off;
		clone.num_layers = this.num_layers;
		clone.numeric_level_off = this.numeric_level_off;
		clone.propMutexes = new HashSet<MutexPair>(this.propMutexes);
		clone.propositionMap = new HashMap<Fact, PlanningGraph.PGFact>(
				this.propositionMap);
		clone.propositions = new HashSet(this.propositions);
		clone.readyActions = new HashSet<PlanningGraph.PGAction>(
				this.readyActions);
		clone.negativePCActions = new HashSet<PGAction>(this.negativePCActions);

		return clone;
	}
	
	
	/**
	 * The creation of a Planning Graph is an extremely costly process in both time and resources. This is
	 * primarily due to the need to recreate information used during the construction process, but for most problems
	 * this information will never change between instances of PG. Therefore, this method creates a new PG while
	 * retaining all this information which would otherwise be destroyed and recreated (such as mutex relationships
	 * etc).
	 * @return An empty PlanningGraph, which has already had its preprocessing information computed/copied from 
	 * this PG.
	 */
	public PlanningGraph branch()
	{
		PlanningGraph branch = new PlanningGraph();
	
		branch.actionMap = this.actionMap;
		branch.actionMutexes = this.actionMutexes;
		branch.actions = this.actions;
//		branch.factLayers = this.factLayers;
//		branch.goal = this.goal;
//		branch.initial = this.initial;
//		branch.level_off = this.level_off;
//		branch.num_layers = this.num_layers;
//		branch.numeric_level_off = this.numeric_level_off;
		branch.propMutexes = this.propMutexes;
		branch.propositionMap = this.propositionMap;
		branch.propositions = this.propositions;
		branch.negativePCActions = this.negativePCActions;
//		branch.readyActions = this.readyActions;
//		branch.state = this.state;
		
		return branch;
	}
	
	/**
	 * Returns the number of fact layers in the graph, including the initial state.
	 * @return
	 */
	public int size()
	{
		return this.num_layers;
	}

	
	public TotalOrderPlan getPlan(State s)
	{
		setInitial(s);
		resetAll(s);
		// AND oldGoal = new AND(this.goal);
		setGoal(s.goal);

		// set up the initial set of facts
		List<PGFact> scheduledFacts = new ArrayList<PGFact>(initial);
		List<PGAction> scheduledActs = null;

		scheduledActs = this.createFactLayer(scheduledFacts, 0);
		List plan = null;

		//can't remember why I did this... so I'm just leaving it. Probably something to do with not destroying object references.
		HashSet<Fact> realInitial = new HashSet<Fact>();
		for (PGFact i : this.initial)
		{
			realInitial.add((Fact) i.getFact());
		}

		this.factLayers.add(realInitial); // add current layer

		// create the graph==========================================
		while (true)
		{
			scheduledFacts = this.createActionLayer(scheduledActs, num_layers);
//			if (scheduledFacts != null && scheduledFacts.isEmpty())
//			{
//				this.level_off = true;
//				break;
//			}
			
			++num_layers;
			scheduledActs = this.createFactLayer(scheduledFacts, num_layers);

			if (scheduledFacts != null)
			{
				HashSet<Fact> factList = new HashSet<Fact>();
				// plan = extractPlan();
				for (Object pgp : scheduledFacts)
					factList.add(((PGFact) pgp).getFact());

				boolean res = factList.addAll(this.factLayers.get(num_layers - 1));
//				if (res == false)
//				{
//					--this.num_layers;
//					this.level_off = true;
//					break; //no new facts added
//				}

				this.factLayers.add(factList); // add current layer

			}
			
//			currentState = this.applyActions(currentState, scheduledActs);

			if (this.goalMet() && !this.goalMutex())
			{
				plan = this.extractPlan();
			}
			if (plan != null)
				break;
			if (!level_off)
				numeric_level_off = 0;
			if (level_off || numeric_level_off >= NUMERIC_LIMIT)
			{
				// printGraph();
				break;
			}
		}

		TotalOrderPlan p = null;
		if (plan != null)
		{
			p = new TotalOrderPlan();
			Iterator pit = plan.iterator();
			while (pit.hasNext())
			{
				PGAction a = (PGAction) pit.next();
				if (!(a instanceof PGNoOp))
					p.addAction(a.getAction());
			}
			// p.print(javaff.JavaFF.infoOutput);
			return p;
		}
		// this.setGoal(oldGoal);

		return p;

	}

	// GET LAYER CONTAINING needs changed to reflect single literals,
	// conjunctions, ors, etc
	public TotalOrderPlan getPlanFromExistingGraph(Fact g)
	{
		readyActions = new HashSet();

		this.setGoal(g);

		List plan = null;
		if (this.goalMet() && !this.goalMutex())
		{
			plan = this.extractPlan();
		}

		TotalOrderPlan p = null;
		if (plan != null)
		{
			p = new TotalOrderPlan();
			Iterator pit = plan.iterator();
			while (pit.hasNext())
			{
				PGAction a = (PGAction) pit.next();
				if (!(a instanceof PGNoOp))
					p.addAction(a.getAction());
			}
			// p.print(javaff.JavaFF.infoOutput);
			return p;
		}
		// this.setGoal(oldGoal);

		return p;
	}

	
	/**
	 * Build the PG until it is fully stable with the specified state (including
	 * goal), but do not construct any kind of plan.
	 * 
	 * @param s
	 * @return
	 */
	//FIXME merge this code into getPlan()/remove duplication
	public void constructStableGraph(State init)
	{
		resetAll(init);
		setInitial(init);
		setGoal(init.goal);

		// set up the initial set of facts
		List<PGFact> scheduledFacts = new ArrayList<PGFact>(this.initial);
		List<PGAction> scheduledActs = null;

		scheduledActs = createFactLayer(scheduledFacts, 0);
		List plan = null;

		//
		HashSet<Fact> realInitial = new HashSet<Fact>();
		for (PGFact i : this.initial)
		{
			realInitial.add((Fact) i.getFact());
		}

		this.factLayers.add(realInitial); // add current layer
		// this.pgFactLayers.add(scheduledFacts); //add current layer

		// create the graph==========================================
		while (true)
		{
			scheduledFacts = createActionLayer(scheduledActs, num_layers);
//			if (scheduledFacts != null && scheduledFacts.isEmpty())
//			{
//				this.level_off = true;
//				break;
//			}
				
			
			++num_layers;
			scheduledActs = createFactLayer(scheduledFacts, num_layers);

			if (scheduledFacts != null)
			{
				HashSet factList = new HashSet();
				// plan = extractPlan();
				for (Object pgp : scheduledFacts)
					factList.add(((PGFact) pgp).getFact());

				boolean res = factList.addAll(this.factLayers.get(num_layers-1));
//				if (res == false)
//				{
//					--this.num_layers;
//					this.level_off = true;
//					break; //no new facts added
//				}
				
				this.factLayers.add(factList); // add current layer

			}

			if (!level_off)
				numeric_level_off = 0;
			if (level_off || numeric_level_off >= NUMERIC_LIMIT)
			{
				// printGraph();
				break;
			}
		}
	}

	public Set<Fact> getFactsAtLayer(int i)
	{
		return this.factLayers.get(i);
	}
	

	public Set<Action> getActionsAtLayer(int l)
	{
		HashSet<Action> app = new HashSet<Action>();
		if (l < 0)
			return app;
		
		for (Entry<Action, PGAction> a : this.actionMap.entrySet())
		{
			if (a.getValue().getLayer() == l)
//			if (a.getValue().layer <= l && a.getValue().layer >= 0)
				app.add(a.getKey());
		
		}
		
		return app;
	}
	

//	public Set<Action> getActionsUpToLayer(int l)
//	{
//		HashSet<Action> app = new HashSet<Action>();
//		for (Entry<Action, PGAction> a : this.actionMap.entrySet())
//		{
//			if (a.getValue().layer <= l)
//				app.add(a.getKey());
//		
//		}
//		
//		return app;
//	}

	/**
	 * Returns the distance/layer which contains the first instance of the
	 * specified proposition.
	 * 
	 * @param p
	 * @return The distance to the proposition, or -1 if it is not found in any
	 *         layer.
	 */
	public int getLayerContaining(Fact p)
	{
		for (int i = 0; i < this.factLayers.size(); i++)
		{
			if (this.factLayers.get(i).contains(p))
				return i;
		}

		return -1;
	}

	public int getFactLayerSize()
	{
		return this.factLayers.size();
	}

	// ******************************************************
	// Setting it all up
	// ******************************************************
	protected void setActionMap(Set<Action> gactions)
	{
		Queue<Action> queue = new LinkedList<Action>(gactions);
		while (queue.isEmpty() == false)
		{
			Action a = queue.remove();

			PGAction pga = new PGAction(a);
			actionMap.put(a, pga);
			actions.add(pga);
		}
	}

	protected PGFact getPGFact(Fact p)
	{

		Object o = propositionMap.get(p);
		PGFact pgp;
		if (o == null)
		{
			pgp = new PGFact(p);
			propositionMap.put(p, pgp);
			propositions.add(pgp);
		}
		else
			pgp = (PGFact) o;
		return pgp;
	}

	protected void setLinks()
	{
		for (PGAction pga : this.actions)
		{
//			//special case for actions which have no preconditions, or whose preconditions
//			//are static and maybe removed by optimisation processes.
//			if (pga.conditions.isEmpty())
//			{
//				this.emptyPreconditions.add(pga);
//			}
			

			boolean onlyNegativePcs = true;
			Set<Fact> pcs = pga.getAction().getPreconditions();
			for (Fact p : pcs)
			{
				PGFact pgp = this.getPGFact(p);
				pga.getConditions().add(pgp);
				pgp.getEnables().add(pga);
				
				//if there is at least one precondition which is positive
				//then it is not worth recording it, as the PG construction
				//will work as normal. Actions which only have negative PCs
				//need special consideration in createFactLayer().
				if (p instanceof Not == false)
				{
					onlyNegativePcs = false;
				}
			}
			if (onlyNegativePcs)
			{
				this.negativePCActions.add(pga);
			}

			Set<Fact> adds = pga.getAction().getAddPropositions();
			for (Fact p : adds)
			{
				PGFact pgp = this.getPGFact(p);
				pga.getAchieves().add(pgp);
				pgp.getAchievedBy().add(pga);
			}

			Set<Not> dels = pga.getAction().getDeletePropositions();
			for (Not p : dels)
			{
				PGFact pgp = this.getPGFact(p);
				pga.getDeletes().add(pgp);
				pgp.getDeletedBy().add(pga);

				//now that negative preconditions are allowed, we have to say that deleting facts
				//actually "achieves" them in order for EHC to work. 
				pga.getAchieves().add(pgp);
				pgp.getAchievedBy().add(pga);
			}
		}
	}

	protected void resetAll(State s)
	{
		factLayers = new ArrayList<Set<Fact>>();

		//TODO should these resets be ignored? If we use another PG to create this one then we're just destroying
		//all the hard work of the previous PG
		propMutexes = new HashSet();
		actionMutexes = new HashSet();

		// memorised = new ArrayList();

		readyActions = new HashSet();

		num_layers = 0;

		Iterator ait = actions.iterator();
		while (ait.hasNext())
		{
			PGAction a = (PGAction) ait.next();
			a.reset();
		}

		Iterator pit = propositions.iterator();
		while (pit.hasNext())
		{
			PGFact p = (PGFact) pit.next();
			p.reset();
		}
	}

	public void setGoal(Fact g)
	{
		goal = new HashSet();
		for (Fact p : g.getFacts())
		{
			PGFact pgp = getPGFact(p);
			goal.add(pgp);
		}
	}

	public void setInitial(State S)
	{
		this.initial = new HashSet<PGFact>();
		
		//always add a TrueCondition to allow empty-precondition actions to execute
		PGFact truePGFact = this.getPGFact(TrueCondition.getInstance());
		initial.add(truePGFact);
		
		for (Fact p : ((STRIPSState) S).getFacts())
		{
			PGFact pgp = this.getPGFact(p);
			this.initial.add(pgp);
		}
	}

	protected void createNoOps()
	{
		for (Object po : this.propositions)
		{
			PGFact p = (PGFact) po;
			PGNoOp n = new PGNoOp(p);
			n.getConditions().add(p);
			n.getAchieves().add(p);
			p.getEnables().add(n);
			p.getAchievedBy().add(n);
			actions.add(n);
		}
	}

	// ******************************************************
	// Graph Construction
	// ******************************************************

	protected ArrayList<PGAction> createFactLayer(List<PGFact> trueFacts, int pLayer)
	{
		// memorised.add(new HashSet());
		ArrayList<PGAction> scheduledActs = new ArrayList<PGAction>();
		HashSet<MutexPair> newMutexes = new HashSet<MutexPair>();
		
//		//first, we add in all actions which have no preconditions and therefore are always applicable.
//		//this has to be done separately because the loop below assumes that there will actually be at-least
//		//one fact true in the current relaxed state. If there are none, and the first action in every plan has
//		//no preconditions, then the RPG will never be constructed.
//		for (Action always : this.emptyPreconditions)
//		{
//			if (pLayer != 0)
//			{
//				Iterator pit = propositions.iterator();
//				while (pit.hasNext())
//				{
//					PGFact p = (PGFact) pit.next();
//					if (p.getLayer() >= 0 && this.checkPropMutex(f, p, pLayer))
//					{
//						this.makeMutex(f, p, pLayer, newMutexes);
//					}
//				}
//			}
//		}
		
		
		//check positive facts
		for (PGFact f : trueFacts)
		{
			if (f.getLayer() < 0)
			{
				//if this fact has never been seen in the PG so far (its layer is < 0), say that it appears at this layer -- this will determine its "difficulty"
				f.setLayer(pLayer);

				//add all actions which this fact enables
				scheduledActs.addAll(f.getEnables());
				 
				level_off = false;

				// calculate mutexes
				if (pLayer != 0)
				{
					Iterator pit = propositions.iterator();
					while (pit.hasNext())
					{
						PGFact p = (PGFact) pit.next();
						if (p.getLayer() >= 0 && this.checkPropMutex(f, p, pLayer))
						{
							this.makeMutex(f, p, pLayer, newMutexes);
						}
					}
				}

			}
		}
		/* 26/10/2012 -- David Pattison
		 * Yet another hack to enable negative preconditions in a PG. Having checked for actions which are
		 * activated using the positive literals in the current state, we need to check for actions which are
		 * activated using *negative* preconditions. This is a subtle bug, as actions which contain at-least 
		 * one positive precondition mask the problem at hand. However, if only negative preconditions are
		 * present in the action spec, then the action can only be activated by the addition, and subsequent
		 * deletion of the required Nots -- when in reality they should be activatable right from the initial state
		 * (assuming none of the Nots are true in this state -- the normal code would work if Nots were present in the
		 * initial state, but this is unlikely due to the sheer number of possibilities).
		 * I freely admit this is not a good way to do this, but the existing infrastructure of JavaFF is not geared towards
		 * non-STRIPS usage, so any solution is better than no solution (pun explicitely and unashamedly intended).
		 */
		STRIPSState hackState = new STRIPSState(); //easier to create a state than duplicate the Not code check for isTrue()
		for (PGFact f : trueFacts)
		{
			hackState.addFact(f.getFact());
		}
		
		//loop through only the actions which we know have ONLY negative preconditions -- no point in checking the others as actions with positive
		//preconditions will always be picked up by the above code.
		for (PGAction a : this.negativePCActions)
		{
			for (PGFact f : a.getConditions())
			{
				if (f.getFact() instanceof Not)
				{
					if (((Not)f.getFact()).isTrue(hackState))
					{
						if (f.getLayer() < 0)
						{
							//if this fact has never been seen in the PG so far (its layer is < 0), say that it appears at this layer -- this will determine its "difficulty"
							f.setLayer(pLayer);

							//add all actions which this fact enables
							scheduledActs.addAll(f.getEnables());
							 
							level_off = false;

							// calculate mutexes
							if (pLayer != 0)
							{
								Iterator pit = propositions.iterator();
								while (pit.hasNext())
								{
									PGFact p = (PGFact) pit.next();
									if (p.getLayer() >= 0 && this.checkPropMutex(f, p, pLayer))
									{
										this.makeMutex(f, p, pLayer, newMutexes);
									}
								}
							}

						}
					}
				}
			}
			
		}

		// check old mutexes
		Iterator pmit = propMutexes.iterator();
		while (pmit.hasNext())
		{
			MutexPair m = (MutexPair) pmit.next();
			if (checkPropMutex(m, pLayer))
			{
				this.makeMutex(m.getNode1(), m.getNode2(), pLayer, newMutexes);
			}
			else
			{
				level_off = false;
			}
		}

		// add new mutexes to old mutexes and remove those which have
		// disappeared
		propMutexes = newMutexes;

		return scheduledActs;
	}

	public boolean checkPropMutex(MutexPair m, int l)
	{
		return checkPropMutex((PGFact) m.getNode1(), (PGFact) m.getNode2(), l);
	}

	public boolean checkPropMutex(Fact p1, Fact p2, int l)
	{
		if (p1 == p2)
			return false;

		PGFact pgp1 = this.getPGFact(p1);
		PGFact pgp2 = this.getPGFact(p2);

		if (pgp1 == null || pgp2 == null)
			return false;

		return this.checkPropMutex(pgp1, pgp2, l);
	}

	protected boolean checkPropMutex(PGFact p1, PGFact p2, int l)
	{
		if (p1 == p2)
			return false;

		// Componsate for statics
		if (p1.getAchievedBy().isEmpty() || p2.getAchievedBy().isEmpty())
			return false;

		Iterator a1it = p1.getAchievedBy().iterator();
		while (a1it.hasNext())
		{
			PGAction a1 = (PGAction) a1it.next();
			if (a1.getLayer() >= 0)
			{
				Iterator a2it = p2.getAchievedBy().iterator();
				while (a2it.hasNext())
				{
					PGAction a2 = (PGAction) a2it.next();
					if (a2.getLayer() >= 0 && !a1.mutexWith(a2, l - 1))
						return false;
				}
			}

		}
		return true;
	}

	protected void makeMutex(Node n1, Node n2, int l, Set<MutexPair> mutexPairs)
	{
		n1.setMutex(n2, l);
		n2.setMutex(n1, l);
		mutexPairs.add(new MutexPair(n1, n2));
	}

	protected ArrayList<PGFact> createActionLayer(List<PGAction> pActions,
			int pLayer)
	{
		level_off = true;
		HashSet<PGAction> actionSet = this
				.getAvailableActions(pActions, pLayer);
		actionSet.addAll(readyActions);
		readyActions = new HashSet<PGAction>();
		HashSet<PGAction> filteredSet = this.filterSet(actionSet, pLayer);
		ArrayList<PGFact> scheduledFacts = this.calculateActionMutexesAndProps(
				filteredSet, pLayer);
		return scheduledFacts;
	}

	/*
	 * 24/5/2011 - David Pattison From what I can tell, this method returns the
	 * set of actions whose preconditions have been satisfied. But this is done
	 * by comparing the number of PCs against the number of actions in the List
	 * provided which satisfy at least one of the PCs. I guess the idea is that
	 * as each PC (PGFact) is mapped to X actions which require it, when the set
	 * of actions which is passed in is constructed, if the layer at p-1
	 * contains N PCs, there will be at least N actions in the list (hence why
	 * it is a list- allows duplicates). If |A| < N, then the actions cannot be
	 * applicable. This is a both a genius and terrible way to do this.
	 */
	protected HashSet<PGAction> getAvailableActions(List<PGAction> pActions,
			int pLayer)
	{
		STRIPSState hackState = new STRIPSState();
		hackState.addFacts(factLayers.get(pLayer));

		HashSet<PGAction> actionSet = new HashSet<PGAction>();
		for (PGAction a : pActions)
		{
			if (a.getLayer() < 0)
			{
				a.setCounter(a.getCounter() + 1);
				a.setDifficulty(a.getDifficulty() + pLayer);
				if (a instanceof PGNoOp || a.getAction().isApplicable(hackState)) //FIXME this breaks non-STRIPS planning
				{
					a.setLayer(pLayer);
					actionSet.add(a);
					level_off = false;
				}
			}
		}
		return actionSet;
	}

	protected HashSet<PGAction> filterSet(Set<PGAction> pActions, int pLayer)
	{
		HashSet<PGAction> filteredSet = new HashSet<PGAction>();
		for (PGAction a : pActions)
		{
			if (this.noMutexes(a.getConditions(), pLayer))
				filteredSet.add(a);
			else
				readyActions.add(a);
		}
		return filteredSet;
	}

	public ArrayList<PGFact> calculateActionMutexesAndProps(
			Set<PGAction> filteredSet, int pLayer)
	{
		HashSet<MutexPair> newMutexes = new HashSet<MutexPair>();

		ArrayList<PGFact> scheduledFacts = new ArrayList<PGFact>();

		for (PGAction a : filteredSet)
		{
			scheduledFacts.addAll(a.getAchieves());
			a.setLayer(pLayer);
			level_off = false;

			// caculate new mutexes
			Iterator a2it = actions.iterator();
			while (a2it.hasNext())
			{
				PGAction a2 = (PGAction) a2it.next();
				if (a2.getLayer() >= 0 && checkActionMutex(a, a2, pLayer))
				{
//					System.out.println("adding action mutex at layer " + pLayer
//							+ "- " + a2 + " <-> " + a);
					this.makeMutex(a, a2, pLayer, newMutexes);
				}
			}
		}
		// check old mutexes
		Iterator amit = actionMutexes.iterator();
		while (amit.hasNext())
		{
			MutexPair m = (MutexPair) amit.next();
			if (checkActionMutex(m, pLayer))
			{
				this.makeMutex(m.getNode1(), m.getNode2(), pLayer, newMutexes);
			}
			else
			{
				level_off = false;
			}
		}

		// add new mutexes to old mutexes and remove those which have
		// disappeared
		actionMutexes = newMutexes;
		return scheduledFacts;
	}

	public boolean checkActionMutex(MutexPair m, int l)
	{
		return checkActionMutex((PGAction) m.getNode1(), (PGAction) m.getNode2(), l);
	}

	public boolean checkActionMutex(PGAction a1, PGAction a2, int l)
	{
		if (a1 == a2)
			return false;

		Iterator p1it = a1.getDeletes().iterator();
		while (p1it.hasNext())
		{
			PGFact p1 = (PGFact) p1it.next();
			;
			if (a2.getAchieves().contains(p1))
				return true;
			if (a2.getConditions().contains(p1))
				return true;
		}

		Iterator p2it = a2.getDeletes().iterator();
		while (p2it.hasNext())
		{
			PGFact p2 = (PGFact) p2it.next();
			if (a1.getAchieves().contains(p2))
				return true;
			if (a1.getConditions().contains(p2))
				return true;
		}

		Iterator pc1it = a1.getConditions().iterator();
		while (pc1it.hasNext())
		{
			PGFact p1 = (PGFact) pc1it.next();
			Iterator pc2it = a2.getConditions().iterator();
			while (pc2it.hasNext())
			{
				PGFact p2 = (PGFact) pc2it.next();
				if (p1.mutexWith(p2, l))
					return true;
			}
		}

		return false;
	}

	protected boolean goalMet()
	{
		for (PGFact p : this.goal)
		{
			if (p.getLayer() < 0)
			{
				return false;
			}
		}
		return true;
	}

	protected boolean goalMutex()
	{
		return !noMutexes(this.goal, num_layers);
	}

	protected boolean noMutexes(Set s, int l)
	{
		Iterator sit = s.iterator();
		if (sit.hasNext())
		{
			Node n = (Node) sit.next();
			HashSet s2 = new HashSet(s);
			s2.remove(n);
			Iterator s2it = s2.iterator();
			while (s2it.hasNext())
			{
				Node n2 = (Node) s2it.next();
				if (n.mutexWith(n2, l))
					return false;
			}
			return noMutexes(s2, l);
		}
		else
			return true;
	}

	protected boolean noMutexesTest(Node n, Set s, int l) // Tests to see if
															// there is a mutex
															// between n and all
															// nodes in s
	{
		Iterator sit = s.iterator();
		while (sit.hasNext())
		{
			Node n2 = (Node) sit.next();
			if (n.mutexWith(n2, l))
				return false;
		}
		return true;
	}

	// ******************************************************
	// Plan Extraction
	// ******************************************************

	public List extractPlan()
	{
		return searchPlan(this.goal, num_layers);
	}

	public List searchPlan(Set goalSet, int l)
	{

		if (l == 0)
		{
			if (initial.containsAll(goalSet))
				return new ArrayList();
			else
				return null;
		}
		// do memorisation stuff
		// Set badGoalSet = (HashSet) memorised.get(l);
		// if (badGoalSet.contains(goalSet))
		// return null;

		List ass = searchLevel(goalSet, (l - 1)); // returns a set of sets of
													// possible action
													// combinations
		Iterator assit = ass.iterator();

		while (assit.hasNext()) // go round each NON-mutex set
		{
			Set as = (HashSet) assit.next();
			Set newgoal = new HashSet();

			Iterator ait = as.iterator();
			while (ait.hasNext())
			{
				PGAction a = (PGAction) ait.next(); // construct a new goal set
													// from the non-mutex action
													// set's effects
				newgoal.addAll(a.getConditions());
			}

			List al = searchPlan(newgoal, (l - 1)); // try to find a plan to
													// this new goal
			if (al != null)
			{
				List plan = new ArrayList(al);
				plan.addAll(as);
				return plan; // if a plan was found, return it, else loop to the
								// next set.
			}

		}

		// do more memorisation stuff
		// badGoalSet.add(goalSet);
		return null;

	}

	public List searchLevel(Set goalSet, int layer)
	{
		if (goalSet.isEmpty())
		{
			Set s = new HashSet();
			List li = new ArrayList();
			li.add(s);
			return li;
		}

		List actionSetList = new ArrayList();
		Set newGoalSet = new HashSet(goalSet);

		Iterator git = goalSet.iterator();
		PGFact g = (PGFact) git.next();
		newGoalSet.remove(g);

		// always prefer No-ops
		for (PGAction a : g.getAchievedBy())
		{
			// System.out.println("Checking "+a+" for no op");
			if ((a instanceof PGNoOp) && a.getLayer() <= layer && a.getLayer() >= 0)
			{
				Set newnewGoalSet = new HashSet(newGoalSet);
				newnewGoalSet.removeAll(a.getAchieves());

				List l = this.searchLevel(newnewGoalSet, layer);

				Iterator lit = l.iterator();
				while (lit.hasNext())
				{
					Set s = (HashSet) lit.next();
					if (this.noMutexesTest(a, s, layer))
					{
						s.add(a);
						actionSetList.add(s);
					}
				}
			}
		}

		for (PGAction a : g.getAchievedBy())
		{
			// ignore no-ops
			if (!(a instanceof PGNoOp) && a.getLayer() <= layer && a.getLayer() >= 0)
			{
				Set newnewGoalSet = new HashSet(newGoalSet); // copy over
																// current goal
																// set
				newnewGoalSet.removeAll(a.getAchieves()); // remove all facts
														// achieved by A
				List l = this.searchLevel(newnewGoalSet, layer);
				Iterator lit = l.iterator();
				while (lit.hasNext())
				{
					Set s = (HashSet) lit.next();
					if (this.noMutexesTest(a, s, layer))
					{
						s.add(a);
						actionSetList.add(s);
					}
				}
			}
		}
		// System.out.println("Found action list for "+g+": "+actionSetList);

		return actionSetList;
	}

	// ******************************************************
	// Useful Methods
	// ******************************************************

	public int getLayer(Action a)
	{
		PGAction pg = (PGAction) actionMap.get(a);
		return pg.getLayer();
	}

	// ******************************************************
	// protected Classes
	// ******************************************************
	public class Node
	{
		private int layer;
		private Set mutexes;

		private Map mutexTable;
		
		//speed up access to hashcodes
		private int hash;

		public Node()
		{
			this.updateHash();
		}
		
		private int updateHash()
		{
			this.hash = super.hashCode();
			
			return this.hash;
		}
		
		@Override
		public int hashCode() 
		{
			return this.hash;
		}

		public void reset()
		{
			setLayer(-1);
			setMutexes(new HashSet(1));
			setMutexTable(new Hashtable(1));
			
			this.updateHash();
		}

		public void setMutex(Node n, int l)
		{
			n.getMutexTable().put(this, new Integer(l));
			this.getMutexTable().put(n, new Integer(l));
			
			this.updateHash();
		}

		public boolean mutexWith(Node n, int l)
		{
			/*
			 * if (this == n) return false; Iterator mit = mutexes.iterator();
			 * while (mit.hasNext()) { Mutex m = (Mutex) mit.next(); if
			 * (m.contains(n)) { return m.layer >= l; } } return false;
			 */
			Object o = getMutexTable().get(n);
			if (o == null)
				return false;
			Integer i = (Integer) o;
			return i.intValue() >= l;
		}

		public int getLayer() {
			return layer;
		}

		public void setLayer(int layer) {
			this.layer = layer;
		}

		public Set getMutexes() {
			return mutexes;
		}

		public void setMutexes(Set mutexes) {
			this.mutexes = mutexes;
		}

		public Map getMutexTable() {
			return mutexTable;
		}

		public void setMutexTable(Map mutexTable) {
			this.mutexTable = mutexTable;
		}
	}

	public class PGAction extends Node
	{
		private Action action;
		private int counter;
		private int difficulty;


		private Set<PGFact> conditions;
		private Set<PGFact> achieves;
		private Set<PGFact> deletes;
		
		private int hash;

		public PGAction()
		{
			this.setConditions(new HashSet<PlanningGraph.PGFact>());
			this.setAchieves(new HashSet<PlanningGraph.PGFact>());
			this.setDeletes(new HashSet<PlanningGraph.PGFact>());
			
			this.updateHash();
		}

		public PGAction(Action a)
		{
			this();
			
			setAction(a);
			
			this.updateHash();
		}
		
		private int updateHash()
		{
			this.hash = super.hashCode();
			if (this.getAction() != null)
				this.hash = this.hashCode() ^ this.getAction().hashCode();// ^ this.getCounter() ^ this.getDifficulty();
			
			return this.hash;
		}
		
		@Override
		public int hashCode() 
		{
			return this.hash;
		}

		public Set getComparators()
		{
			return getAction().getComparators();
		}

		public Set getOperators()
		{
			return getAction().getOperators();
		}

		public void reset()
		{
			super.reset();
			setCounter(0);
			setDifficulty(0);
			
			this.updateHash();
		}

		public String toString()
		{
			return getAction().toString();
		}

		public Action getAction() {
			return action;
		}

		public void setAction(Action action) {
			this.action = action;
			this.updateHash();
		}

		public int getCounter() {
			return counter;
		}

		public void setCounter(int counter) {
			this.counter = counter;
		}

		public int getDifficulty() {
			return difficulty;
		}

		public void setDifficulty(int difficulty) {
			this.difficulty = difficulty;
		}

		public Set<PGFact> getConditions() {
			return conditions;
		}

		public void setConditions(Set<PGFact> conditions) {
			this.conditions = conditions;
		}

		public Set<PGFact> getAchieves() {
			return achieves;
		}

		public void setAchieves(Set<PGFact> achieves) {
			this.achieves = achieves;
		}

		public Set<PGFact> getDeletes() {
			return deletes;
		}

		public void setDeletes(Set<PGFact> deletes) {
			this.deletes = deletes;
		}
	}

	protected static final HashSet EmptySet = new HashSet(0);
	public class PGNoOp extends PGAction
	{
		private PGFact proposition;
		
		private int hash;

		public PGNoOp(PGFact p)
		{
			setProposition(p);
			
			this.updateHash();
		}

		public String toString()
		{
			return ("No-Op " + getProposition());
		}

		public Set getComparators()
		{
			return PlanningGraph.EmptySet;
		}

		public Set getOperators()
		{
			return PlanningGraph.EmptySet;
		}
		
		private int updateHash()
		{
			this.hash = super.hashCode() ^ this.getProposition().hashCode();
			return this.hash;
		}
		
		@Override
		public int hashCode() 
		{
			return this.hash;
		}

		public PGFact getProposition() {
			return proposition;
		}

		public void setProposition(PGFact proposition) {
			this.proposition = proposition;
			this.updateHash();
		}
	}

	public class PGFact extends Node
	{
		private Fact fact;

		private Set<PGAction> enables;
		private Set<PGAction> achievedBy;
		private Set<PGAction> deletedBy;
		
		private int hash;
		
		private PGFact()
		{
			this.setEnables(new HashSet<PGAction>());
			this.setAchievedBy(new HashSet<PGAction>());
			this.setDeletedBy(new HashSet<PGAction>());
		}

		public PGFact(Fact p)
		{
			this();
			
			setFact(p);
			
			this.updateHash();
		}
		
		private int updateHash()
		{
			this.hash = super.hashCode() ^ this.getFact().hashCode();
			return this.hash;
		}
		
		@Override
		public int hashCode() 
		{
			return this.hash;
		}

		public String toString()
		{
			return getFact().toString();
		}

		public Fact getFact() {
			return fact;
		}

		public void setFact(Fact fact) {
			this.fact = fact;
			this.updateHash();
		}

		public Set<PGAction> getEnables() {
			return enables;
		}

		public void setEnables(Set<PGAction> enables) {
			this.enables = enables;
		}

		public Set<PGAction> getAchievedBy() {
			return achievedBy;
		}

		public void setAchievedBy(Set<PGAction> achievedBy) {
			this.achievedBy = achievedBy;
		}

		public Set<PGAction> getDeletedBy() {
			return deletedBy;
		}

		public void setDeletedBy(Set<PGAction> deletedBy) {
			this.deletedBy = deletedBy;
		}

		// public Object clone()
		// {
		// PGFact clone = new PGFact((Fact) fact.clone());
		// clone.enables = new HashSet<PlanningGraph.PGAction>();
		// clone.
		// }
	}

	protected class MutexPair
	{
		private Node node1;
		private Node node2;
		
		private int hash;

		public MutexPair(Node n1, Node n2)
		{
			setNode1(n1);
			setNode2(n2);
			
			this.updateHash();
		}
		
		private int updateHash()
		{
			this.hash = getNode1().hashCode() ^ getNode2().hashCode();
			return this.hash;
		}
		
		@Override
		public int hashCode() 
		{
			return this.hash;
		}

		public Node getNode1() {
			return node1;
		}

		public void setNode1(Node node1) {
			this.node1 = node1;
			this.updateHash();
		}

		public Node getNode2() {
			return node2;
		}

		public void setNode2(Node node2) {
			this.node2 = node2;
			this.updateHash();
		}
	}

	// ******************************************************
	// Debugging Classes
	// ******************************************************
	public void printGraph()
	{
		for (int i = 0; i <= num_layers; ++i)
		{
			System.out.println("-----Layer " + i
					+ "----------------------------------------");
			printLayer(i);
		}
		System.out
				.println("-----End -----------------------------------------------");
	}

	public void printLayer(int i)
	{
		System.out.println("Facts:");
		Iterator pit = propositions.iterator();
		while (pit.hasNext())
		{
			PGFact p = (PGFact) pit.next();
			if (p.getLayer() <= i && p.getLayer() >= 0)
			{
				System.out.println("\t" + p);
				System.out.println("\t\tmutex with");
				Iterator mit = p.getMutexTable().keySet().iterator();
				while (mit.hasNext())
				{
					PGFact pm = (PGFact) mit.next();
					Integer il = (Integer) p.getMutexTable().get(pm);
					if (il.intValue() >= i)
					{
						System.out.println("\t\t\t" + pm);
					}
				}
			}
		}
		if (i == num_layers)
			return;
		System.out.println("Actions:");
		Iterator ait = actions.iterator();
		while (ait.hasNext())
		{
			PGAction a = (PGAction) ait.next();
			if (a.getLayer() <= i && a.getLayer() >= 0)
			{
				System.out.println("\t" + a);
				System.out.println("\t\tmutex with");
				Iterator mit = a.getMutexTable().keySet().iterator();
				while (mit.hasNext())
				{
					PGAction am = (PGAction) mit.next();
					Integer il = (Integer) a.getMutexTable().get(am);
					if (il.intValue() >= i)
					{
						System.out.println("\t\t\t" + am);
					}
				}
			}
		}
	}

}