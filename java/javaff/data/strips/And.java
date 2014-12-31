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
import javaff.data.GroundFact;

import javaff.data.GroundFact;
import javaff.data.UngroundFact;

import javaff.data.CompoundLiteral;
import javaff.data.PDDLPrinter;
import javaff.planning.State;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.io.PrintStream;

public class And extends AbstractCompoundLiteral implements CompoundLiteral, GroundFact, UngroundFact, STRIPSFact
{
	public And()
	{
		super();
		this.literals = new HashSet<Fact>();
	}

	public And(Collection<Fact> props)
	{
		this();
		for (Fact f : props)
			this.add(f);
	}
	
	public And(Fact f)
	{
		this();
		this.add(f);
	}
	
	public Object clone()
	{
		And and = new And();
		and.literals = new HashSet<Fact>(this.literals);
		return and;
	}

	public boolean isTrue(State s)
	{
		for (Fact f : this.literals)
		{
			GroundFact c = (GroundFact) f;
			if (!c.isTrue(s))
				return false;
		}
		return true;
	}

	public void apply(State s)
	{
		applyDels(s);
		applyAdds(s);
	}

	public void applyAdds(State s)
	{
		Iterator eit = literals.iterator();
		while (eit.hasNext())
		{
			GroundFact e = (GroundFact) eit.next();
			e.applyAdds(s);
		}
	}

	public void applyDels(State s)
	{
		Iterator eit = literals.iterator();
		while (eit.hasNext())
		{
			GroundFact e = (GroundFact) eit.next();
			e.applyDels(s);
		}
	}

	public boolean effects(PredicateSymbol ps)
	{
		boolean rEff = false;
		Iterator lit = literals.iterator();
		while (lit.hasNext() && !(rEff))
		{
			UngroundFact ue = (UngroundFact) lit.next();
			rEff = ue.effects(ps);
		}
		return rEff;
	}

	public UngroundFact minus(UngroundFact effect)
	{
		And a = new And();
		Iterator lit = literals.iterator();
		while (lit.hasNext())
		{
			UngroundFact p = (UngroundFact) lit.next();
			a.add(p.minus(effect));
		}
		return a;
	}

	public UngroundFact effectsAdd(UngroundFact cond)
	{
		Iterator lit = literals.iterator();
		UngroundFact c = null;
		while (lit.hasNext())
		{
			UngroundFact p = (UngroundFact) lit.next();
			UngroundFact d = p.effectsAdd(cond);
			if (!d.equals(cond))
				c = d;
		}
		if (c == null)
			return cond;
		else
			return c;
	}

	public GroundFact ground(Map<Variable, PDDLObject> varMap)
	{
		And a = new And();
		Iterator lit = literals.iterator();
		while (lit.hasNext())
		{
			UngroundFact p = (UngroundFact) lit.next();
			GroundFact g = p.ground(varMap);
			
			if (g instanceof And)
			{
				for (Fact f : g.getFacts())
				{
					a.add(f);
				}				
			}
			else
				a.add(g);
		}
		return a;
	}

	public boolean equals(Object obj)
	{
		if (obj instanceof And)
		{
			And a = (And) obj;
			return (literals.equals(a.literals));
		} else
			return false;
	}

	public int hashCode()
	{
		return literals.hashCode() ^ 864;
	}

	public void PDDLPrint(PrintStream p, int indent)
	{
		PDDLPrinter.printToString(literals, "and", p, false, true, indent);
	}

	public String toString()
	{
		String str = "(and";
		Iterator it = literals.iterator();
		while (it.hasNext())
		{
			Object next = it.next();
			if (next instanceof TrueCondition || next instanceof NullFact)
				continue;
			
			str += " (" + next+") ";
		}
		str += ")";
		return str;
	}

	public String toStringTyped()
	{
		String str = "(and";
		Iterator it = literals.iterator();
		while (it.hasNext())
		{
			Object next = it.next();
			if (next instanceof Not)
			{
				Not l = (Not) next;
				str += " (" + l.toStringTyped()+")";
			}
			else if (next instanceof TrueCondition || next instanceof NullFact)
			{
			}
			else
			{
				Literal l = (Literal) next;
				str += " (" + l.toStringTyped()+")";
			}
		}
		str += ")";
		return str;

	}
}
