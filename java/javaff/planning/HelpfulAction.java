package javaff.planning;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javaff.data.Action;
import javaff.data.Fact;
import javaff.data.Parameter;
import javaff.data.metric.NamedFunction;
import javaff.data.strips.InstantAction;
import javaff.data.strips.Not;
import javaff.data.strips.OperatorName;
import javaff.data.strips.STRIPSInstantAction;


/**
 * Wrapper class for comparing helpful actions. In the classic FF sense, helpful actions are those
 * which are applicable at layer 0 of the RPG and add at-least one goal literal at layer 1. Note that these
 * are not the "final" goals, but rather intermediate goals which appear during relaxed plan extraction in the RPG.
 * 
 * This wrapper allows helpful actions to be sorted, based upon how many goal literals they achieve. If another
 * helpful action achieves the same number of goals, they are sorted alphabetically. If they both have the same
 * result of calling toString(), the world implodes.
 * 
 * @author David Pattison
 *
 */

public class HelpfulAction extends STRIPSInstantAction implements Comparable<HelpfulAction>
{
	private int goalsAchieved;
	private Action action;

	public int getGoalsAchieved()
	{
		return goalsAchieved;
	}

	public void setGoalsAchieved(int goalsAchieved)
	{
		this.goalsAchieved = goalsAchieved;
	}
	
	public String toString()
	{
		return action.toString();
	}

	public boolean isApplicable(State s)
	{
		return action.isApplicable(s);
	}

	public void apply(State s)
	{
		action.apply(s);
	}

	public Set<Fact> getPreconditions()
	{
		return action.getPreconditions();
	}

	public Set<Fact> getAddPropositions()
	{
		return action.getAddPropositions();
	}

	public Set<Not> getDeletePropositions()
	{
		return action.getDeletePropositions();
	}

	public Set<NamedFunction> getComparators()
	{
		return action.getComparators();
	}

	public Set getOperators()
	{
		return action.getOperators();
	}

	public void staticify(Map fValues)
	{
		action.staticify(fValues);
	}

	public boolean deletes(Fact f)
	{
		return action.deletes(f);
	}

	public boolean adds(Fact f)
	{
		return action.adds(f);
	}

	public boolean requires(Fact f)
	{
		return action.requires(f);
	}

	public boolean equals(Object obj)
	{
		return action.equals(obj) && this.goalsAchieved == ((HelpfulAction)obj).goalsAchieved;
	}

	public int hashCode()
	{
		return action.hashCode();
	}

	public Object clone()
	{
		HelpfulAction clone = new HelpfulAction((Action) this.action.clone(), this.goalsAchieved);
		return clone;
	}

	public List<Parameter> getParameters()
	{
		return action.getParameters();
	}

	public void setParameters(List<Parameter> parameters)
	{
		action.setParameters(parameters);
	}

	public OperatorName getName()
	{
		return action.getName();
	}

	public void setName(OperatorName name)
	{
		action.setName(name);
	}
	
	/**
	 * Compares 2 helpful actions. If this helpful action achieves a higher number of goals than the other, -1 is
	 * returned, or +1 if the opposite is true. If both achieve the same number of goals, the result is
	 * dictated by {@link String#compareTo(String)}, where the strings compared are the result of calling toString() on 
	 * both actions. In practice, it is assumed that both actions will never have the same result of calling
	 * toString(), i.e. that 0 is returned. 
	 * @see #getGoalsAchieved()
	 * @see String#compareTo(String)
	 */
	@Override
	public int compareTo(HelpfulAction o)
	{
		if (this.getGoalsAchieved() > o.getGoalsAchieved())
			return -1;
		else if (this.getGoalsAchieved() == o.getGoalsAchieved())
		{
			int res = this.toString().compareTo(o.toString());
			
			//in an ADL-world, we may have a single ADL action which was decompiled
			//into multiple STRIPS actions -- which would probably have the same action signature
			//Therefore, if 0 is returned (action signatures are the same) we have to do a full comparison
			//of the entire action structure. The easiest way to do this is just to generate the hashCode() for
			//each action and compare these instead. This is a relatively slow process so we only do it as-needed,
			//rather than being the default comparison method.
			if (res == 0)
			{
				Integer aHash = this.hashCode();
				Integer bHash = o.hashCode();
				int fullRes = aHash.compareTo(bHash);
				
				return fullRes;
			}
			else
				return res;
		}
		else 
			return +1;
	}
	
	public HelpfulAction(Action a, int goalsAchieved)
	{
		super();
		this.action = a;
		this.goalsAchieved = goalsAchieved;
	}
}
