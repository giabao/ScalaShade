/*
 * Copyright 2015 Kevin Jones
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.keng.scalashade;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helper for classes that may contain a @ScalaSignature annotation
 */
class ScalaSigClass {

    private static final String SCALA_LONG_SIGNATURE_DESC = "Lscala/reflect/ScalaLongSignature;";
    private static final String SCALA_SIGNATURE_DESC = "Lscala/reflect/ScalaSignature;";

    private final ClassNode _clazz = new ClassNode();
    private int sigAnnotation = -1;
    private ScalaSig sig = null;

    /**
     * Create from path to class file
     *
     * @param path the class file
     */
    public ScalaSigClass(String path) {
        FileInputStream in;
        try {
            in = new FileInputStream(path);
        } catch (IOException e) {
            throw new CtxException("Could not open/read file: " + path);
        }
        load(path, in);
    }

    /**
     * Create from path and input stream
     *
     * @param path path of class, just for error reporting
     * @param in   stream of class byte code
     */
    public ScalaSigClass(String path, InputStream in) {
        load(path, in);
    }

    /**
     * Private constructor, loads the class & parses @ScalaSignature if present
     *
     * @param path path of class, just for error reporting
     * @param in   stream of class byte code
     */
    private void load(String path, InputStream in) {

        // Load class into ASM
        try {
            ClassReader cr = new ClassReader(in);
            cr.accept(_clazz, 0);
        } catch (IOException e) {
            throw new CtxException("Could not read file: " + path);
        }

        // Extract ScalaSignature annotation bytes & check all looks OK
        int at = 0;
        if (_clazz.visibleAnnotations != null) {
            for (AnnotationNode an : visibleAnnotations(_clazz)) {
                if (an.desc.equals(SCALA_LONG_SIGNATURE_DESC)) {
                    validateSignature(an, path);
                    loadSignature(an, path);
                    sigAnnotation = at;
                } else if (an.desc.equals(SCALA_SIGNATURE_DESC)) {
                    validateSignature(an, path);
                    loadSignature(an, path);
                    sigAnnotation = at;
                }
                at++;
            }
        }
    }

    private void validateSignature(AnnotationNode annotation, String path) {
        if (sigAnnotation != -1)
            throw new CtxException("Multiple ScalaSignature annotations found in: " + path);
        if (annotation.values.size() != 2)
            throw new CtxException("ScalaSignature has wrong number of values in: " + path);
        if (!(annotation.values.get(0) instanceof String))
            throw new CtxException("ScalaSignature has wrong type for value 0 in: " + path);
        if (!annotation.values.get(0).equals("bytes"))
            throw new CtxException("ScalaSignature has wrong first value in " + path);
    }

    private void loadSignature(AnnotationNode annotation, String path) {
        String signatureString = "";
        if (annotation.desc.equals(SCALA_LONG_SIGNATURE_DESC)) {
            if (!(annotation.values.get(1) instanceof List)) {
                throw new CtxException("ScalaSignature has wrong type for value 1 in: " + path);
            }
            for (String part : ((List<String>) annotation.values.get(1))) {
                System.out.println(part.length());
                signatureString += part;
            }
        } else {
            if (!(annotation.values.get(1) instanceof String)) {
                throw new CtxException("ScalaSignature has wrong type for value 1 in: " + path);
            }
            signatureString = (String) annotation.values.get(1);
        }
        byte[] signatureBytes = Encoding.decode(signatureString);
        if (null == signatureBytes) {
            throw new CtxException("ScalaSignature could not be decoded in " + path);
        }
        sig = ScalaSig.parse(signatureBytes);
    }

    /**
     * Get access to the @ScalaSignature
     *
     * @return ScalaSig or null if no @ScalaSignature present
     */
    public ScalaSig getSig() {
        return sig;
    }

    /**
     * Write the class byte to a file, will include any modification to @ScalaSignature
     *
     * @param path where to write
     * @throws CtxException
     */
    public void writeTo(String path) throws CtxException {
        try {
            FileOutputStream os = new FileOutputStream(path);
            os.write(getBytes());
            os.close();
        } catch (IOException ex) {
            throw new CtxException("Could not open/read file: " + path);
        }
    }

    /**
     * Get class bytes, will include any modification to @ScalaSignature
     *
     * @return the (possibly updated) class byte code
     */
    public byte[] getBytes() {
    	ArrayList<String> splits = splits(Encoding.encode(sig.asBytes()));

        // Update annotation
        if (sigAnnotation != -1) {
            if (splits.size() == 1) {
                setAnnotation(_clazz, sigAnnotation, splits.get(0));
            } else {
                setAnnotation(_clazz, sigAnnotation, splits);
            }
        }

        // Convert to byte code
        ClassWriter cw = new ClassWriter(0);
        _clazz.accept(cw);
        return cw.toByteArray();
    }
    
    /* According to: http://www.scala-lang.org/old/sites/default/files/sids/dubochet/Mon,%202010-05-31,%2015:25/Storage%20of%20pickled%20Scala%20signatures%20in%20class%20files.pdf
     * MAX_SPLIT_SIZE should be 65535
     * Yet for my case it didn't work.
     * The largest number that works for okapi shade use case is 65493. */
    private static final int MAX_SPLIT_SIZE = 65493;
    private ArrayList<String> splits(String encoded){
    	
    	ArrayList<String> splits = new ArrayList<>();
    	int splitCount = (encoded.length()/MAX_SPLIT_SIZE) + 1;
    	for(int i=0;i<splitCount;i++){
    		splits.add(
    			encoded.substring(
    				i*MAX_SPLIT_SIZE, 
    				Math.min(
    					(i+1)*MAX_SPLIT_SIZE,
    					encoded.length()
    				)
    			)
    		);
    	}
    	return splits;
    }

    private static List<AnnotationNode> visibleAnnotations(ClassNode clazz) {
        return clazz.visibleAnnotations;
    }

    private static <T> void setAnnotation(ClassNode clazz, int index, T content) {
        AnnotationNode an = visibleAnnotations(clazz).get(index);
        if (content instanceof List && an.desc.equals(SCALA_SIGNATURE_DESC)) {
            AnnotationNode longAN = new AnnotationNode(SCALA_LONG_SIGNATURE_DESC);
            longAN.values = Arrays.asList("bytes", content);
        } else {
            an.values.set(1, content);
        }
    }

}
