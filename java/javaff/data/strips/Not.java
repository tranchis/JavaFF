/************************************************************************
 * Strathclyde Planning Group,
 * Department of Computer and Information Sciences,
 * University of Strathclyde, Glasgow, UK
 * http://planning.cis.strath.ac.uk/
 * 
 * Copyright 2007, Keith Halsey
 * Copyright 2008, Andrew Coles and Amanda Smith
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

package javaff.data.strips;

import javaff.data.Fact;
import javaff.data.GroundFact;
import javaff.data.Literal;
import javaff.data.PDDLPrinter;
import javaff.data.CompoundLiteral;


import javaff.data.UngroundFact;
import javaff.data.metric.NamedFunction;
import javaff.planning.State;
import javaff.planning.STRIPSState;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * NOT wrapper for anything derived from Fact. NOT itself is derived from this too.
 * @author David Pattison
 *
 */
public class Not extends Literal implements GroundFact, UngroundFact, STRIPSFact
{
	protected static final HashSet EmptySet = new HashSet();
	
	public Fact literal;

	public Not(Fact l)
	{
//		if (l instanceof Not)
//			this.literal = new Not(((Not)l).literal);
//		else
			this.literal = l;
	}
	
	@Override
	public Set<Fact> getFacts()
	{
		Set<Fact> s = new HashSet<Fact>(1);
		s.add(this);
		return s;
	}
	
	public Object clone()
	{
		return new Not((Fact) this.literal.clone());
	}

	public void apply(State s)
	{
		applyDels(s);
		applyAdds(s);
	}

	/*
	 * In an ADL world, delete effects are considered Achieved in the state
	 */
	public void applyAdds(State s)
	{
		STRIPSState ss = (STRIPSState) s;
		ss.addFact(this);
	}

	public void applyDels(State s)
	{		
		STRIPSState ss = (STRIPSState) s;
		ss.removeFact(this.literal);
		
	}

	public boolean effects(PredicateSymbol ps)
	{
		UngroundFact ue = (UngroundFact) literal;
		return ue.effects(ps);
	}

	public UngroundFact effectsAdd(UngroundFact cond)
	{
		return cond;
	}
//	public Set getAddPropositions()
//	{
//		return NOT.EmptySet;
//	}

//	public Set getDeletePropositions()
//	{
//		Set rSet = new HashSet();
//		rSet.add(literal);
//		return rSet;
//	}

	public Set getOperators()
	{
		return Not.EmptySet;
	}

	public boolean equals(Object obj)
	{
		if (obj instanceof Not)
		{
			Not n = (Not) obj;
			return (literal.equals(n.literal));
		} else
			return false;
	}

	@Override
	protected int updateHash()
	{
		this.hash = super.hashCode() ^ literal.hashCode() ^ 2;
		return this.hash;
	}

	public String toString()
	{
		return "not (" + literal.toString() + ")";
	}

	public String toStringTyped()
	{
		return "not (" + literal.toStringTyped() + ")";
	}

	public void PDDLPrint(java.io.PrintStream p, int indent)
	{
		p.print("(not ");
		PDDLPrinter.printToString(literal, p, false, true, indent);
		p.print(")");
	}

	@Override
	public Set<NamedFunction> getComparators()
	{
		return ((GroundFact)this.literal).getComparators();
	}

//	@Override
//	public Set<Fact> getConditionalPropositions()
//	{
//		return ((GroundFact)this.literal).getConditionalPropositions();
//	}

	@Override
	public boolean isTrue(State s)
	{
		/*
		 * 12/2/2013
		 * This test is complicated. Not technically, but mentally. It performs 3 tests:
		 * 1 - Is this NOT explicitly present in the current state? If so, return true.
		 * 2 - Is the fact which this NOT negates true in the state? If so, return false.
		 * 3 - Otherwise, just return true. This is because NOT which is not true in the current
		 * state, and whose encapsulated literal is not true, must by absence, be true. That is, 
		 * if something is not explicitly true, and it is not negated by anything, then 
		 * it is true by virtue of not being enumerated anywhere...
		 * Basically, it is stupid to enumerate all negated literals, so anything which 
		 * conditions 1 and 2 do not capture, is true because the absence of the fact is the same
		 * as saying it is explicitly not true.
		 * I hope you understand, because I don't. 
		 */
		
		//test if this NOT is explicitly included in the state
		boolean thisInState = ((STRIPSState)s).getFalseFacts().contains(this);
		if (thisInState)
			return true;
		
//		//else, test for whether the negated literal is true...
		boolean literalInState = ((GroundFact)this.literal).isTrue(s);
		if (literalInState)
			return false;
		

		return true;
	}

	@Override
	public GroundFact staticify()
	{
//		return ((GroundFact)this.literal).staticify(fValues);
		GroundFact staticed = ((GroundFact)this.literal).staticify();
		if (staticed instanceof Not)
			return staticed;
		else
			return new Not(staticed);
//		return this;
	}

	@Override
	public boolean isStatic()
	{
//		return false;
		return ((GroundFact)this.literal).isStatic();
	}
	
	public void setStatic(boolean value)
	{
		this.literal.setStatic(value);

		this.updateHash();
	}

	@Override
	public Set getStaticPredicates()
	{
		return ((UngroundFact)this.literal).getStaticPredicates();
	}

	@Override
	public GroundFact ground(Map<Variable, PDDLObject> varMap)
	{
		return new Not(((UngroundFact)this.literal).ground(varMap));
	}

	@Override
	public UngroundFact minus(UngroundFact effect)
	{
		return ((UngroundFact)this.literal).minus(effect);
	}
}
