"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""

import glob
import json
import os
from resource_management import *

def flume(action = None):
  import params

  flume_agents = {}
  if params.flume_conf_content is not None:
    flume_agents = build_flume_topology(params.flume_conf_content)

  agent_names = flume_agents.keys()
  if len(params.flume_command_targets) > 0:
    agent_names = params.flume_command_targets

  if action == 'config':
    Directory(params.flume_conf_dir)
    Directory(params.flume_log_dir, owner=params.flume_user)

    for agent in flume_agents.keys():
      flume_agent_conf_dir = params.flume_conf_dir + os.sep + agent
      flume_agent_conf_file = flume_agent_conf_dir + os.sep + 'flume.conf'
      flume_agent_meta_file = flume_agent_conf_dir + os.sep + 'ambari-meta.json'
      flume_agent_log4j_file = flume_agent_conf_dir + os.sep + 'log4j.properties'

      Directory(flume_agent_conf_dir)

      PropertiesFile(flume_agent_conf_file,
        properties=flume_agents[agent],
        mode = 0644)

      File(flume_agent_log4j_file,
        content=Template('log4j.properties.j2', agent_name = agent),
        mode = 0644)

      File(flume_agent_meta_file,
        content = json.dumps(ambari_meta(agent, flume_agents[agent])),
        mode = 0644)

  elif action == 'start':
    flume_base = format('env JAVA_HOME={java_home} /usr/bin/flume-ng agent '
      '--name {{0}} '
      '--conf {{1}} '
      '--conf-file {{2}} '
      '{{3}}')

    for agent in agent_names:
      flume_agent_conf_dir = params.flume_conf_dir + os.sep + agent
      flume_agent_conf_file = flume_agent_conf_dir + os.sep + "flume.conf"
      flume_agent_pid_file = params.flume_run_dir + os.sep + agent + ".pid"

      if not os.path.isfile(flume_agent_conf_file):
        continue

      if not is_live(flume_agent_pid_file):
        # TODO someday make the ganglia ports configurable
        extra_args = ''
        if params.ganglia_server_host is not None:
          extra_args = '-Dflume.monitoring.type=ganglia -Dflume.monitoring.hosts={0}:{1}'
          extra_args = extra_args.format(params.ganglia_server_host, '8655')

        flume_cmd = flume_base.format(agent, flume_agent_conf_dir,
           flume_agent_conf_file, extra_args)

        Execute(flume_cmd, wait_for_finish=False)

        # sometimes startup spawns a couple of threads - so only the first line may count
        pid_cmd = format('pgrep -o -f {flume_agent_conf_file} > {flume_agent_pid_file}')

        Execute(pid_cmd, logoutput=True, tries=5, try_sleep=10)

    pass
  elif action == 'stop':
    pid_files = glob.glob(params.flume_run_dir + os.sep + "*.pid")

    if 0 == len(pid_files):
      return

    if len(agent_names) > 0:
      for agent in agent_names:
        pid_file = params.flume_run_dir + os.sep + agent + '.pid'
        pid = format('`cat {pid_file}` > /dev/null 2>&1')
        Execute(format('kill {pid}'), ignore_failures=True)
        File(pid_file, action = 'delete')
    else:
      for pid_file in pid_files:
        pid = format('`cat {pid_file}` > /dev/null 2>&1')
        Execute(format('kill {pid}'), ignore_failures=True)
        File(pid_file, action = 'delete')
    

def ambari_meta(agent_name, agent_conf):
  res = {}

  sources = agent_conf[agent_name + '.sources'].split(' ')
  res['sources_count'] = len(sources)

  sinks = agent_conf[agent_name + '.sinks'].split(' ')
  res['sinks_count'] = len(sinks)

  channels = agent_conf[agent_name + '.channels'].split(' ')
  res['channels_count'] = len(channels)

  return res

# define a map of dictionaries, where the key is agent name
# and the dictionary is the name/value pair
def build_flume_topology(content):

  result = {}
  agent_names = []

  for line in content.split('\n'):
    rline = line.strip()
    if 0 != len(rline) and not rline.startswith('#'):
      pair = rline.split('=')
      lhs = pair[0].strip()
      rhs = pair[1].strip()

      part0 = lhs.split('.')[0]

      if lhs.endswith(".sources"):
        agent_names.append(part0)

      if not result.has_key(part0):
        result[part0] = {}

      result[part0][lhs] = rhs

  # trim out non-agents
  for k in result.keys():
    if not k in agent_names:
      del result[k]


  return result

def is_live(pid_file):
  live = False

  try:
    check_process_status(pid_file)
    live = True
  except ComponentIsNotRunning:
    pass

  return live

def live_status(pid_file):
  import params

  pid_file_part = pid_file.split(os.sep).pop()

  res = {}
  res['name'] = pid_file_part
  
  if pid_file_part.endswith(".pid"):
    res['name'] = pid_file_part[:-4]

  res['status'] = 'RUNNING' if is_live(pid_file) else 'NOT_RUNNING'
  res['sources_count'] = 0
  res['sinks_count'] = 0
  res['channels_count'] = 0

  flume_agent_conf_dir = params.flume_conf_dir + os.sep + res['name']
  flume_agent_meta_file = flume_agent_conf_dir + os.sep + 'ambari-meta.json'

  try:
    with open(flume_agent_meta_file) as fp:
      meta = json.load(fp)
      res['sources_count'] = meta['sources_count']
      res['sinks_count'] = meta['sinks_count']
      res['channels_count'] = meta['channels_count']
  except:
    pass

  return res
  
def flume_status():
  import params

  # these are what Ambari believes should be running
  meta_files = glob.glob(params.flume_conf_dir + os.sep + "*/ambari-meta.json")
  pid_files = []
  for meta_file in meta_files:
    agent_name = os.path.dirname(meta_file).split(os.sep).pop()
    pid_files.append(os.path.join(params.flume_run_dir, agent_name + '.pid'))

  procs = []
  for pid_file in pid_files:
    procs.append(live_status(pid_file))

  return procs

