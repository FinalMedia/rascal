/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:

 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Anastasia Izmaylova - A.Izmaylova@cwi.nl - CWI
*******************************************************************************/
package org.rascalmpl.interpreter.types;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public class OverloadedFunctionType extends RascalType {
	private final Set<FunctionType> alternatives;
	private final Type returnType;
	
	private static final TypeFactory TF = TypeFactory.getInstance();
	private static final RascalTypeFactory RTF = RascalTypeFactory.getInstance();

	/*package*/ OverloadedFunctionType(Set<FunctionType> alternatives) {
		this.alternatives = alternatives;
		this.returnType = alternatives.iterator().next().getReturnType();
	}
	
	public int size() {
		return alternatives.size();
	}
	
	public Type getReturnType() {
		return returnType;
	}
	
	@Override
	public <T, E extends Throwable> T accept(IRascalTypeVisitor<T, E> visitor) throws E {
	  return visitor.visitOverloadedFunction(this);
	}
	
	@Override
	protected boolean isSupertypeOf(RascalType type) {
	  return type.isSubtypeOfOverloadedFunction(this);
	}
	
	@Override
	protected Type lub(RascalType type) {
	  return type.lubWithOverloadedFunction(this);
	}
	
	@Override
	protected Type glb(RascalType type) {
	  return type.glbWithOverloadedFunction(this);
	}
	
	public Set<FunctionType> getAlternatives() {
		return Collections.unmodifiableSet(alternatives);
	}
	
	@Override
	protected boolean isSubtypeOfOverloadedFunction(RascalType type) {
	  OverloadedFunctionType of = (OverloadedFunctionType) type;

	  // if this has at least one alternative that is a sub-type of the other, 
	  // then yes, this function can act as the other and should be a sub-type
	  
	  // TODO: this is broken because of defaults. We should distinguish!
	  for(FunctionType f : getAlternatives()) {
	    if(f.isSubtypeOf(of)) {
	      return true;
	    }
	  }

	  return false;
	}
	
	@Override
	protected boolean isSubtypeOfFunction(RascalType type) {
	// TODO: this is broken because of defaults. We should distinguish!
	  
	  for (FunctionType a : alternatives) {
	    if (a.isSubtypeOf(type)) {
	      return true;
	    }
	  }
	  return false;
	}
	
	@Override
	protected Type lubWithOverloadedFunction(RascalType type) {		
	  if(this == type) {
	    return this;
	  }
	  
	  OverloadedFunctionType of = (OverloadedFunctionType) type;

	  Set<FunctionType> newAlternatives = new HashSet<FunctionType>();
	  
	  for(FunctionType f : getAlternatives()) {
		  for(FunctionType g : of.getAlternatives()) {
			  Type lub = f.lubWithFunction(g);
			  if(lub instanceof FunctionType)
				  newAlternatives.add((FunctionType)lub);
		  }
	  }
	  
	  if(!newAlternatives.isEmpty())
		  return RTF.overloadedFunctionType(newAlternatives);
	  
	  return TF.valueType();
	}
		
	@Override
	protected Type lubWithFunction(RascalType type) {
	  FunctionType f = (FunctionType) type;

	  Set<FunctionType> newAlternatives = new HashSet<FunctionType>();
	  newAlternatives.add(f);
	  
	  return this.lubWithOverloadedFunction((RascalType)RTF.overloadedFunctionType(newAlternatives));
	}
	
	@Override
	protected Type glbWithOverloadedFunction(RascalType type) {
	  if(this == type) {
		return this;
	  }
		  
	  OverloadedFunctionType of = (OverloadedFunctionType) type;
		  
	  Set<FunctionType> newAlternatives = new HashSet<FunctionType>();
	  
	  if(getReturnType() == of.getReturnType()) {
	    newAlternatives.addAll(getAlternatives());
	    newAlternatives.addAll(of.getAlternatives());
	    return RTF.overloadedFunctionType(newAlternatives);
	  }
	  
	  Type returnType = getReturnType().glb(of.getReturnType());
		  
	  for(FunctionType f : getAlternatives()) {
	      newAlternatives.add((FunctionType)RTF.functionType(returnType, f.getArgumentTypes()));
	  }
		  
	  for(FunctionType f : of.getAlternatives()) {
		  newAlternatives.add((FunctionType)RTF.functionType(returnType, f.getArgumentTypes()));
	  }
		  
	  return RTF.overloadedFunctionType(newAlternatives);
	}

	@Override
	protected Type glbWithFunction(RascalType type) {
	  FunctionType f = (FunctionType) type;

	  Set<FunctionType> newAlternatives = new HashSet<FunctionType>();
	  newAlternatives.add(f);
		  
	  return this.glbWithOverloadedFunction((RascalType)RTF.overloadedFunctionType(newAlternatives));
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		if (obj.getClass().equals(getClass())) {
			OverloadedFunctionType f = (OverloadedFunctionType) obj;
			return alternatives.equals(f.alternatives);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		// TODO: better hashCode?
		return alternatives.hashCode();
	}
	
	@Override
	public String toString() {
		return getReturnType() + " (...)";
	}
	
	@Override
	public Type compose(Type right) {
		if (right.isBottom()) {
			return right;
		}
		Set<FunctionType> newAlternatives = new HashSet<FunctionType>();
		if(right instanceof FunctionType) {
			for(FunctionType ftype : this.alternatives) {
				if(TF.tupleType(((FunctionType) right).getReturnType()).isSubtypeOf(ftype.getArgumentTypes())) {
					newAlternatives.add((FunctionType) RTF.functionType(ftype.getReturnType(), ((FunctionType) right).getArgumentTypes()));
				}
			}
		} else if(right instanceof OverloadedFunctionType) {
			for(FunctionType ftype : ((OverloadedFunctionType) right).getAlternatives()) {
				for(FunctionType gtype : this.alternatives) {
					if(TF.tupleType(ftype.getReturnType()).isSubtypeOf(gtype.getArgumentTypes())) {
						newAlternatives.add((FunctionType) RTF.functionType(gtype.getReturnType(), ftype.getArgumentTypes()));
					}
				}
			}
		} else {
			throw new IllegalOperationException("compose", this, right);
		}
		if(!newAlternatives.isEmpty()) 
			return RTF.overloadedFunctionType(newAlternatives);
		return TF.voidType();
	}

}
