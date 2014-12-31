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
import javaff.planning.State;
import javaff.planning.STRIPSState;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class Proposition extends javaff.data.Literal implements GroundFact, SingleLiteral, STRIPSFact
{
	public Proposition(PredicateSymbol p)
	{
		super();
		name = p;
		
		this.updateHash();
	}
	
	protected Proposition()
	{
		name = new PredicateSymbol();
		
		this.updateHash();
	}
	
	@Override
	protected int updateHash()
	{
		this.hash = super.updateHash() ^ 11;
		return this.hash;
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
		Proposition p = new Proposition(this.name);
		p.parameters = new ArrayList(this.parameters);
		
		p.updateHash();
		
		return p;
	}

	public boolean isTrue(State s) // returns whether this conditions is true
									// is State S
	{
		STRIPSState ss = (STRIPSState) s;
		boolean t = ss.isTrue(this);
		return t;
	}

	public void apply(State s) // carry out the effects of this on State s
	{
		applyDels(s);
		applyAdds(s);
	}

	public void applyAdds(State s)
	{
		STRIPSState ss = (STRIPSState) s;
		ss.addFact(this);
	}

	public void applyDels(State s)
	{
		STRIPSState ss = (STRIPSState) s;
		ss.removeFact(new Not(this)); //TODO hack for removing negated version of propositions
	}

	public boolean isStatic()
	{
		return name.isStatic();
	}

	public GroundFact staticify()
	{
		if (isStatic())
			return TrueCondition.getInstance();
		else
			return this;
	}

	public Set getOperators()
	{
		return super.EmptySet;
	}

	public Set getComparators()
	{
		return super.EmptySet;
	}
	
	@Override
	public boolean equals(Object obj)
	{
//		return this.toString().equals(obj.toString());
		if (obj instanceof Proposition)
		{
			Proposition p = (Proposition) obj;
			return (name.equals(p.name) && parameters.equals(p.parameters));
		} 
		else
			return false;
	}
}
