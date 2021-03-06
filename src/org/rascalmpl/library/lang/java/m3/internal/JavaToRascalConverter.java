package org.rascalmpl.library.lang.java.m3.internal;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.rascalmpl.value.IConstructor;
import org.rascalmpl.value.IList;
import org.rascalmpl.value.IListWriter;
import org.rascalmpl.value.ISourceLocation;
import org.rascalmpl.value.IValue;
import org.rascalmpl.value.IValueFactory;
import org.rascalmpl.value.type.TypeFactory;
import org.rascalmpl.value.type.TypeStore;
import org.rascalmpl.values.ValueFactoryFactory;

public abstract class JavaToRascalConverter extends ASTVisitor {
	protected static final IValueFactory values = ValueFactoryFactory.getValueFactory();
	protected static final TypeFactory TF = TypeFactory.getInstance();

	protected final TypeStore typeStore;
	
	protected IValue ownValue;
	private static final String DATATYPE_RASCAL_AST_TYPE_NODE 			= "Type";
	private static final String DATATYPE_RASCAL_AST_MODIFIER_NODE 		= "Modifier";
	private static final String DATATYPE_RASCAL_AST_DECLARATION_NODE 	= "Declaration";
	private static final String DATATYPE_RASCAL_AST_EXPRESSION_NODE 	= "Expression";
	private static final String DATATYPE_RASCAL_AST_STATEMENT_NODE 		= "Statement";
	private static final String DATATYPE_RASCAL_MESSAGE                 = "Message";
	private static final String DATATYPE_RASCAL_MESSAGE_ERROR           = "error";
	
	private final org.rascalmpl.value.type.Type DATATYPE_RASCAL_AST_DECLARATION_NODE_TYPE;
	private final org.rascalmpl.value.type.Type DATATYPE_RASCAL_AST_EXPRESSION_NODE_TYPE;
	private final org.rascalmpl.value.type.Type DATATYPE_RASCAL_AST_STATEMENT_NODE_TYPE;
	protected static org.rascalmpl.value.type.Type DATATYPE_RASCAL_AST_TYPE_NODE_TYPE;
	protected static org.rascalmpl.value.type.Type DATATYPE_RASCAL_AST_MODIFIER_NODE_TYPE;
	protected static org.rascalmpl.value.type.Type DATATYPE_RASCAL_MESSAGE_DATA_TYPE;
	protected static org.rascalmpl.value.type.Type DATATYPE_RASCAL_MESSAGE_ERROR_NODE_TYPE;
	
	protected CompilationUnit compilUnit;
	protected ISourceLocation loc;
	
	protected final BindingsResolver bindingsResolver;
	protected final boolean collectBindings;
	
	protected IListWriter messages;
    protected final Map<String, ISourceLocation> locationCache;

	JavaToRascalConverter(final TypeStore typeStore, Map<String, ISourceLocation> cache, boolean collectBindings) {
		super(true);
		this.typeStore = typeStore;
		this.bindingsResolver = new BindingsResolver(typeStore, cache, collectBindings);
		this.collectBindings = collectBindings;
		DATATYPE_RASCAL_AST_TYPE_NODE_TYPE 		= this.typeStore.lookupAbstractDataType(DATATYPE_RASCAL_AST_TYPE_NODE);
		DATATYPE_RASCAL_AST_MODIFIER_NODE_TYPE = this.typeStore.lookupAbstractDataType(DATATYPE_RASCAL_AST_MODIFIER_NODE);
		this.DATATYPE_RASCAL_AST_DECLARATION_NODE_TYPE 	= typeStore.lookupAbstractDataType(DATATYPE_RASCAL_AST_DECLARATION_NODE);
		this.DATATYPE_RASCAL_AST_EXPRESSION_NODE_TYPE 	= typeStore.lookupAbstractDataType(DATATYPE_RASCAL_AST_EXPRESSION_NODE);
		this.DATATYPE_RASCAL_AST_STATEMENT_NODE_TYPE 	= typeStore.lookupAbstractDataType(DATATYPE_RASCAL_AST_STATEMENT_NODE);
		JavaToRascalConverter.DATATYPE_RASCAL_MESSAGE_DATA_TYPE          = typeStore.lookupAbstractDataType(DATATYPE_RASCAL_MESSAGE);
		JavaToRascalConverter.DATATYPE_RASCAL_MESSAGE_ERROR_NODE_TYPE    = typeStore.lookupConstructor(DATATYPE_RASCAL_MESSAGE_DATA_TYPE, DATATYPE_RASCAL_MESSAGE_ERROR).iterator().next();
		this.locationCache = cache;

		messages = values.listWriter();
	}
	
	protected ISourceLocation resolveBinding(String packageComponent) {
		ISourceLocation packageBinding = new BindingsResolver(typeStore, locationCache, this.collectBindings) {
			public ISourceLocation resolveBinding(String packageC) {
				try {
					if (collectBindings) {
						if (locationCache.containsKey(packageC)) {
							return locationCache.get(packageC);
						}
						return values.sourceLocation("java+package", null, packageC);
					}
					return values.sourceLocation("unknown", null, null);
				} catch (URISyntaxException e) {
					throw new RuntimeException("Should not happen", e);
				}
			}
		}.resolveBinding(packageComponent);
		locationCache.put(packageComponent, packageBinding);
		return packageBinding;
	}
	
	protected ISourceLocation resolveBinding(CompilationUnit node) {
		ISourceLocation compilationUnit = new BindingsResolver(typeStore, locationCache, true) {
			public ISourceLocation resolveBinding(CompilationUnit node) {
				return makeBinding("java+compilationUnit", null, loc.getPath());
			}
		}.resolveBinding(node);
		
		return compilationUnit;
	}
	
	protected ISourceLocation resolveBinding(IBinding binding) {
		ISourceLocation resolvedBinding = bindingsResolver.resolveBinding(binding);
		if (binding != null)
			locationCache.put(binding.getKey(), resolvedBinding);
		return resolvedBinding;
	}
	
	protected ISourceLocation resolveDeclaringClass(IBinding binding) {
		ISourceLocation resolvedBinding;
		if (binding instanceof ITypeBinding) {
		  resolvedBinding = bindingsResolver.resolveBinding(((ITypeBinding) binding).getDeclaringClass());
		} else if (binding instanceof IMethodBinding) {
		  resolvedBinding = bindingsResolver.resolveBinding(((IMethodBinding) binding).getDeclaringClass());
		} else if (binding instanceof IVariableBinding) {
		  resolvedBinding = bindingsResolver.resolveBinding(((IVariableBinding) binding).getDeclaringClass());
		} else {
			binding = null;
			resolvedBinding = bindingsResolver.resolveBinding(binding);
		}
		return resolvedBinding;
	}
	
	protected ISourceLocation resolveBinding(ASTNode node) {
		if (node instanceof CompilationUnit) {
			return resolveBinding((CompilationUnit) node);
		}
		return bindingsResolver.resolveBinding(node, true);
	}
	
	protected ISourceLocation getSourceLocation(ASTNode node) {
		try {
			int nodeLength = compilUnit.getExtendedLength(node);
			
			if (nodeLength > 0) {
				int start = compilUnit.getExtendedStartPosition(node);
				int end = start + nodeLength -1;
				
				if (end < start && ((node.getFlags() & 9) > 0)) {
					insert(messages, values.constructor(DATATYPE_RASCAL_MESSAGE_ERROR_NODE_TYPE,
							values.string("Recovered/Malformed node, guessing the length"),
							values.sourceLocation(loc, 0, 0)));

					nodeLength = node.toString().length();
					end = start + nodeLength - 1;
				}
		
				return values.sourceLocation(loc, 
						 start, nodeLength, 
						 compilUnit.getLineNumber(start), compilUnit.getLineNumber(end), 
						 // TODO: only adding 1 at the end seems to work, need to test.
						 compilUnit.getColumnNumber(start), compilUnit.getColumnNumber(end)+1);
			}
		} catch (IllegalArgumentException e) {
			insert(messages, values.constructor(DATATYPE_RASCAL_MESSAGE_ERROR_NODE_TYPE,
					values.string("Most probably missing dependency"),
					values.sourceLocation(loc, 0, 0)));
		}
		return values.sourceLocation(loc, 0, 0, 0, 0, 0, 0);
	}
	
	protected IValue[] removeNulls(IValue... withNulls) {
		List<IValue> withOutNulls = new ArrayList<IValue>();
		for (IValue child : withNulls) {
			if (!(child == null)) {
        withOutNulls.add(child);
      }
		}
		return withOutNulls.toArray(new IValue[withOutNulls.size()]);
	}
	
	protected IValueList parseModifiers(int modifiers) {
		IValueList extendedModifierList = new IValueList(values);
		
		
		for (String constructor: java.lang.reflect.Modifier.toString(modifiers).split(" ")) {
			Set<org.rascalmpl.value.type.Type> exConstr = typeStore.lookupConstructor(DATATYPE_RASCAL_AST_MODIFIER_NODE_TYPE, constructor);
			for (org.rascalmpl.value.type.Type con: exConstr) {
				extendedModifierList.add(values.constructor(con));
			}
		}
		
		return extendedModifierList;
	}
	
	@SuppressWarnings({"rawtypes"})
	protected IValueList parseExtendedModifiers(List ext) {
		IValueList extendedModifierList = new IValueList(values);
	
		for (Iterator it = ext.iterator(); it.hasNext();) {
			ASTNode p = (ASTNode) it.next();
			IValue val = visitChild(p);
			if (p instanceof Annotation) {
        val = constructModifierNode("annotation", val);
      }
			extendedModifierList.add(val);
		}
		return extendedModifierList;
	}
	
	@SuppressWarnings("deprecation")
	protected IValueList parseExtendedModifiers(BodyDeclaration node) {
		if (node.getAST().apiLevel() == AST.JLS2) {
			return parseModifiers(node.getModifiers());
		} else {
			return parseExtendedModifiers(node.modifiers());
		}
	}
	
	protected IValue visitChild(ASTNode node) {
		node.accept(this);
		return this.getValue();
	}

	public IValue getValue() {
		return this.ownValue;
	}
	
	protected IConstructor constructModifierNode(String constructor, IValue... children) {
		org.rascalmpl.value.type.Type args = TF.tupleType(removeNulls(children));
		org.rascalmpl.value.type.Type constr = typeStore.lookupConstructor(DATATYPE_RASCAL_AST_MODIFIER_NODE_TYPE, constructor, args);
		return values.constructor(constr, removeNulls(children));
	}
	
	protected void setAnnotation(String annoName, IValue annoValue) {
		if(this.ownValue == null) {
      return ;
    }
		if (annoValue != null && ownValue.getType().declaresAnnotation(this.typeStore, annoName)) {
      ownValue = ((IConstructor) ownValue).asAnnotatable().setAnnotation(annoName, annoValue);
    }
	}
	
	protected void setAnnotation(String annoName, IValueList annoList) {
		IList annos = (IList) annoList.asList();
		if(this.ownValue == null) {
      return ;
    }
		if (annoList != null && this.ownValue.getType().declaresAnnotation(this.typeStore, annoName) && !annos.isEmpty()) {
      this.ownValue = ((IConstructor) this.ownValue).asAnnotatable().setAnnotation(annoName, annos);
    }
	}
	
	protected IValue constructDeclarationNode(String constructor, IValue... children) {
		org.rascalmpl.value.type.Type args = TF.tupleType(removeNulls(children));
		org.rascalmpl.value.type.Type constr = typeStore.lookupConstructor(DATATYPE_RASCAL_AST_DECLARATION_NODE_TYPE, constructor, args);
		return values.constructor(constr, removeNulls(children));
	}
	
	protected IValue constructExpressionNode(String constructor, IValue... children) {
		org.rascalmpl.value.type.Type args = TF.tupleType(removeNulls(children));
		org.rascalmpl.value.type.Type constr = typeStore.lookupConstructor(DATATYPE_RASCAL_AST_EXPRESSION_NODE_TYPE, constructor, args);
		return values.constructor(constr, removeNulls(children));
	}
	
	protected IValue constructStatementNode(String constructor, IValue... children) {
		org.rascalmpl.value.type.Type args = TF.tupleType(removeNulls(children));
		org.rascalmpl.value.type.Type constr = typeStore.lookupConstructor(DATATYPE_RASCAL_AST_STATEMENT_NODE_TYPE, constructor, args);
		return values.constructor(constr, removeNulls(children));
	}
	
	protected IValue constructTypeNode(String constructor, IValue... children) {
		org.rascalmpl.value.type.Type args = TF.tupleType(removeNulls(children));
		org.rascalmpl.value.type.Type constr = typeStore.lookupConstructor(DATATYPE_RASCAL_AST_TYPE_NODE_TYPE, constructor, args);
		return values.constructor(constr, removeNulls(children));
	}
	
	protected void insertCompilationUnitMessages(boolean insertErrors, IList otherMessages) {
		org.rascalmpl.value.type.Type args = TF.tupleType(TF.stringType(), TF.sourceLocationType());
		
		IValueList result = new IValueList(values);

		if (otherMessages != null) {
			for (IValue message : otherMessages) {
				result.add(message);
			}
		}

		if (insertErrors) {
			int i;
	
			IProblem[] problems = compilUnit.getProblems();
			for (i = 0; i < problems.length; i++) {
				int offset = problems[i].getSourceStart();
				int length = problems[i].getSourceEnd() - offset + 1;
				int sl = problems[i].getSourceLineNumber();
				ISourceLocation pos = values.sourceLocation(loc, offset, length, sl, sl, 0, 0);
				org.rascalmpl.value.type.Type constr;
				if (problems[i].isError()) {
					constr = typeStore.lookupConstructor(this.typeStore.lookupAbstractDataType("Message"), "error", args);
				} else {
					constr = typeStore.lookupConstructor(this.typeStore.lookupAbstractDataType("Message"), "warning", args);
				}
				result.add(values.constructor(constr, values.string(problems[i].getMessage()), pos));
			}
		}
		setAnnotation("messages", result.asList());
	}

	public void insert(IListWriter listW, IValue message) {
		if (message.getType().isConstructor() && message.getType().getAbstractDataType().getName().equals("Message")) {
			listW.insert(message);
		}
	}
	
    public void convert(CompilationUnit root, ASTNode node, ISourceLocation loc) {
        this.compilUnit = root;
        this.loc = loc;
        node.accept(this);
    }
}
