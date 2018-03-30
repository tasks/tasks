/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

/**
 * Constants for backup XML attributes and nodes.
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class BackupConstants {

  // Do NOT edit the constants in this file! You will break compatibility with old backups

  // --- general xml

  /** Tag containing Astrid backup data */
  public static final String ASTRID_TAG = "astrid";

  /** Attribute indicating backup file format */
  public static final String ASTRID_ATTR_FORMAT = "format";

  // --- format 2

  /** Tag containing a task */
  public static final String TASK_TAG = "task";

  /** Tag containing a comment item */
  public static final String COMMENT_TAG = "comment";

  /** Tag containing a metadata item */
  public static final String METADATA_TAG = "metadata";

  /** Tag containing a tagdata item */
  public static final String TAGDATA_TAG = "tagdata";

  // --- general

  public static final String EXPORT_FILE_NAME = "user.%s.json";

  public static final String BACKUP_FILE_NAME = "auto.%s.json";

  public static final String UPGRADE_FILE_NAME = "upgradefrom.%s.json";
}
