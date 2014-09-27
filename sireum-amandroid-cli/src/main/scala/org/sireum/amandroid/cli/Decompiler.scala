/*
Copyright (c) 2013-2014 Fengguo Wei & Sankardas Roy, Kansas State University.        
All rights reserved. This program and the accompanying materials      
are made available under the terms of the Eclipse Public License v1.0 
which accompanies this distribution, and is available at              
http://www.eclipse.org/legal/epl-v10.html                             
*/
package org.sireum.amandroid.cli

import org.sireum.option.SireumAmandroidDecompileMode
import org.sireum.option.DumpSource
import org.sireum.jawa.util.APKFileResolver
import org.sireum.util.FileUtil
import java.io.File
import org.sireum.util.FileResourceUri
import java.net.URI
import org.sireum.amandroid.cli.util.CliLogger
import org.sireum.amandroid.decompile.Dex2PilarConverter

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 */

object DecompilerCli {
	def run(sadmode : SireumAmandroidDecompileMode) {
    val sourceType = sadmode.typ match{
      case DumpSource.APK => "APK"
      case DumpSource.DIR => "DIR"
      case DumpSource.DEX => "DEX"}
    val sourceDir = sadmode.srcFile
    val sourceFile = new File(sourceDir)
    val outputDirOpt = if(sadmode.outFile == "") None else Some(sadmode.outFile)
    val outputDir = outputDirOpt match{
	    case Some(path) => path
	    case None => sourceFile.getParent()
	  }
    forkProcess(sourceType, sourceDir, outputDir)
  }
	
	def forkProcess(typSpec : String, sourceDir : String, outputDir : String) = {
	  val args = List("-t", typSpec, sourceDir, outputDir)
    org.sireum.jawa.util.JVMUtil.startSecondJVM(Decompiler.getClass(), "-Xmx2G", args, true)
  }
}

object Decompiler {
	def main(args: Array[String]) {
	  if(args.size != 4){
	    println("Usage: -t type[allows: APK, DIR, DEX] <source path> <output path>")
	    return
	  }
	  val typ = args(1)
	  val sourcePath = args(2)
	  val outputPath = args(3)
	  val outputUri = FileUtil.toUri(outputPath)
	  val dexFileUris = typ match{
      case "APK" =>
        require(sourcePath.endsWith(".apk"))
        val apkFileUri = FileUtil.toUri(sourcePath)
        Set(APKFileResolver.getDexFile(apkFileUri, outputUri))
      case "DIR" =>
        require(new File(sourcePath).isDirectory())
        val apkFileUris = FileUtil.listFiles(FileUtil.toUri(sourcePath), ".apk", true).toSet
        apkFileUris.map{
		      apkFileUri=>
		        APKFileResolver.getDexFile(apkFileUri, outputUri)
		    }
      case "DEX" =>
        require(sourcePath.endsWith(".dex") || sourcePath.endsWith(".odex"))
        Set(FileUtil.toUri(sourcePath))
      case _ => 
        println("Unexpected type: " + typ)
        return
    }
    decompile(dexFileUris, outputPath)
  }
	
	def decompile(dexFileUris : Set[FileResourceUri], outputPath : String) = {
    dexFileUris.foreach{
      dexFileUri =>
        try{
          Dex2PilarConverter.convert(dexFileUri)
        } catch {
          case e : Throwable =>
            CliLogger.logError(new File(outputPath), "Error: " , e)
        }
    }
	}
}