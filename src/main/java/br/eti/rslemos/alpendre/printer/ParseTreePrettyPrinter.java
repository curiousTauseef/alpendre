/*******************************************************************************
 * BEGIN COPYRIGHT NOTICE
 * 
 * This file is part of program "alpendre"
 * Copyright 2016  Rodrigo Lemos
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * END COPYRIGHT NOTICE
 ******************************************************************************/
package br.eti.rslemos.alpendre.printer;

import java.io.PrintStream;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

public class ParseTreePrettyPrinter {

	char[][] model = { 
		//   0123
			"╮  ╭".toCharArray(), // 0 - MANY-CHILD
			"╰┬─╯".toCharArray(), // 1 - MANY-CHILD 
			" │  ".toCharArray(), // 2 - PARALLEL LINES
			" ├╴╶".toCharArray(), // 3 - ONE-CHILD
			" ╰╴ ".toCharArray(), // 4 - LAST-CHILD
		};
	
//	char[][] model = { 
//		//   0123
//			"-   ".toCharArray(), // 0 - MANY-CHILD
//			" |  ".toCharArray(), // 1 - MANY-CHILD 
//			" |  ".toCharArray(), // 2 - PARALLEL LINES
//			" +--".toCharArray(), // 3 - ONE-CHILD
//			" \\- ".toCharArray(), // 4 - LAST-CHILD
//			};
	
	private final PrintStream out;
	private final String[] ruleNames;

	private String positionFormat;
	private String positionEmpty;

	public enum ConfigTreeInline {
		ALWAYS,
		ONLY_DIRECT_LINE,
		NEVER,
	}

	public ConfigTreeInline configTreeInline = ConfigTreeInline.ALWAYS;
	
	public ParseTreePrettyPrinter(PrintStream out, String[] ruleNames) {
		this.out = out;
		this.ruleNames = ruleNames;
	}

	public void printTree(ParseTree node) {
		// sweep tree to discover
		int lineWidth = 5;
		int columnWidth = 3; 
		
		positionFormat = "(%" + lineWidth + "d:%" + columnWidth + "d) ";
		positionEmpty = String.format(" %" + lineWidth + "s %" + columnWidth + "s  ", "", "");
		
		printPosition(node);
		printTree("", node);
	}
	
	private void printPosition(ParseTree node) {
		Token token = getFirstTerminal(node);
		if (token != null) {
			int line = token.getLine();
			int column = token.getCharPositionInLine();
			out.printf(positionFormat, line, column);
		} else
			out.print(positionEmpty);
	}

	private Token getFirstTerminal(ParseTree node) {
		try {
			if (node.getChildCount() == 0)
				return (Token)node.getPayload();
		} catch (ClassCastException e) {}
		
		for (int i = 0; i < node.getChildCount(); i++) {
			try {
				return getFirstTerminal(node.getChild(i));
			} catch (ClassCastException e) {}
		}
		
		return null;
	}

	private void printTree(String prefix, ParseTree node) {
		if (node instanceof RuleContext)
			printNT(prefix, (RuleContext)node);
		else
			printT(node);
	}

	private void printT(ParseTree node) {
		out.print(model[4][2]);
		out.println(node.getText());
	}

	private boolean isDirectLine(RuleContext node) {
		if (node.getChildCount() == 1) {
			ParseTree child = node.getChild(0);

			return !(child instanceof RuleContext) || isDirectLine((RuleContext)child);
		}
		
		return node.getChildCount() == 0;
	}

	private void printNT(String prefix, RuleContext node) {
		String rule = ruleNames[node.getRuleIndex()];
		
		int ntChildren = countNonTerminalChildren(node);
		
		if (ntChildren > 0) {
			
			if (configTreeInline == ConfigTreeInline.NEVER || node.getChildCount() > 1 || 
				(configTreeInline == ConfigTreeInline.ONLY_DIRECT_LINE && !isDirectLine(node))
			) {
				out.print(model[0][0]);
				out.print(rule);
				out.println(model[0][3]);
				
				out.print(positionEmpty);
				out.print(prefix);
				out.print(model[1][0]);
				out.print(model[1][1]);
				out.print(TokenPrettyPrinter.repeat(model[1][2], rule.length()-1));
				out.println(model[1][3]);

				int i;
				for (i = 0; i < node.getChildCount()-1; i++) {
					printPosition(node.getChild(i));
					String newPrefix = printPrefix(prefix, false);
					printTree(newPrefix, node.getChild(i));
				}
					
				printPosition(node.getChild(i));
				String newPrefix = printPrefix(prefix, true);
				printTree(newPrefix, node.getChild(i));
				
			} else {
				// special case for just one child: inline its tree
				out.print(model[3][2]);
				out.print(rule);
				out.print(model[3][3]);
				
				String newPrefix = prefix + TokenPrettyPrinter.repeat(' ', rule.length()+2);
				printTree(newPrefix, node.getChild(0));
			}
		} else {
			out.print(model[4][2]);
			out.print(rule);
			
			if (node.getChildCount() > 0) {
				out.print(model[3][3]);
				out.print(model[3][2]);
				
				int i;
				for (i = 0; i < node.getChildCount()-1; i++) {
					out.print(node.getChild(i).getText());
					out.print(' ');
				}

				out.print(node.getChild(i).getText());
			}
			
			out.println();
		}
	}

	private String printPrefix(String prefix, boolean last) {
		out.print(prefix);
		out.print(' ');
		out.print(!last ? model[3][1] : model[4][1]);
		return prefix + ' ' + (!last ? model[2][1] : ' ');
	}

	private int countNonTerminalChildren(ParseTree node) {
		int childRuleCount = 0;
		for (int i = 0; i < node.getChildCount(); i++) {
			if (node.getChild(i) instanceof RuleContext)
				childRuleCount++;
		}
		return childRuleCount;
	}

}
