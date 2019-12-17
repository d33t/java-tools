/* Copyright (C) <2019> <Rusi Rusev> rusev@aemdev.de */
package net.demonsteam.tools.ascii;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import net.demonsteam.tools.ascii.tree.Tree;
import net.demonsteam.tools.ascii.tree.impl.AsciiTreeImpl;

/**
 * Examples <br>
 * Single line (split by comma ',') <br>
 *
 * <pre>
 * {@code
 * /content/a/aa/aac,/content/a/aa/aac/jcr:content,/content/a/aa/aac/jcr:content/-propa1,/content/a/aa/aac/jcr:content/-propa2,/content/a/aa/jcr:content,/content/a/aa/aac/jcr:content/ca,/content/a/aa/aac/jcr:content/cb,/content/a/aa/jcr:content/-propb1,/content/a/aa/jcr:content/-propb2,/content/a/aa/jcr:content/ba,/content/b/ba/jcr:content/c,/content/b/ba/baa/jcr:content/a,/content/b/ba/baa/jcr:content/b,/content/b/ba/baa/jcr:content/b/-propc1,/libs/a,/libs/b
 * EOF
 * }
 * </pre>
 *
 * <br>
 * Multi lines (split by comma '\n') <br>
 *
 * <pre>
 * {@code
 * /content/project/lang/page/jcr:content/section/-sling:resourceType="my/resource/type1"
 * /content/project/lang/page/jcr:content/section/parsys/-sling:resourceType="my/resource/type2"
 * /content/project/lang/page/jcr:content/section/parsys/component1
 * /content/project/lang/page/jcr:content/section/parsys/component2
 * /content/project/lang/page/jcr:content/section/parsys/...
 * EOF
 * }
 * </pre>
 *
 * <br>
 * The user input ends with the char sequence EOF. <br>
 * A root may be added with the special sequence #ROOT= and than relative paths (but also absolute paths possible)
 *
 * <pre>
 * {@code
 * #ROOT=/content/project/lang/page/jcr:content/section
 * -sling:resourceType="my/resource/type1"
 * parsys/-sling:resourceType="my/resource/type2"
 * parsys/component1
 * /content/project/lang/page/jcr:content/section/parsys/component2
 * #ROOT=/content/project/lang/page/jcr:content/section1
 * parsys/component3
 * ...
 * EOF
 * }
 * </pre>
 *
 * @author Rusi Rusev <rusev@aemdev.de>
 * @date 13 Nov 2019
 */
public class TextPathToAscii {

	public static void main(String[] args) {

		try (Scanner in = new Scanner(System.in)) {
			List<String> data = new ArrayList<>();
			while(in.hasNext()) {
				String line = in.nextLine();
				if(line.trim().equals("EOF")) {
					break;
				}
				data.add(line);
			}

			String userInput = String.join("\n", data.toArray(new String[data.size()]));
			String[] paths;
			if(userInput.contains(",")) {
				paths = userInput.split(",");
			} else {
				paths = userInput.split("\n");
			}
			Tree<String> tree = new AsciiTreeImpl("root", paths, false);
			tree.print();
		}
	}
}
