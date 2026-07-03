import React, { useMemo } from 'react';
import StatusBadge from './StatusBadge';

const getNodeStyles = (status) => {
  const normalized = status ? status.toUpperCase() : 'PENDING';
  switch (normalized) {
    case 'RUNNING':
      return 'border-amber-500 shadow-[0_0_15px_rgba(245,158,11,0.5)]';
    case 'COMPLETED':
      return 'border-emerald-500';
    case 'FAILED':
      return 'border-red-500';
    case 'QUEUED':
      return 'border-blue-500';
    case 'PENDING':
    default:
      return 'border-gray-500';
  }
};

export default function DAGVisualizer({ tasks = [], onNodeClick }) {
  const layoutedTasks = useMemo(() => {
    if (!tasks || tasks.length === 0) return [];

    const taskMap = new Map();
    tasks.forEach(t => taskMap.set(t.name, { ...t, level: 0 }));

    // Calculate topological levels
    let changed = true;
    while (changed) {
      changed = false;
      taskMap.forEach(task => {
        const deps = task.dependencies || task.dependsOn || [];
        let maxDepLevel = -1;
        deps.forEach(depName => {
          const dep = taskMap.get(depName);
          if (dep && dep.level > maxDepLevel) {
            maxDepLevel = dep.level;
          }
        });
        
        if (maxDepLevel + 1 > task.level) {
          task.level = maxDepLevel + 1;
          changed = true;
        }
      });
    }

    // Group tasks by their level
    const levelGroups = {};
    taskMap.forEach(task => {
      if (!levelGroups[task.level]) levelGroups[task.level] = [];
      levelGroups[task.level].push(task);
    });

    // Calculate X and Y coordinates
    const CONTAINER_HEIGHT = 600;
    const X_OFFSET = 150;
    const X_SPACING = 250;
    const result = [];

    Object.keys(levelGroups).forEach(levelStr => {
      const level = parseInt(levelStr);
      const levelTasks = levelGroups[level];
      const columnX = X_OFFSET + level * X_SPACING;
      const verticalSpacing = CONTAINER_HEIGHT / (levelTasks.length + 1);

      levelTasks.forEach((task, index) => {
        task.x = columnX;
        task.y = verticalSpacing * (index + 1);
        result.push(task);
      });
    });

    return result;
  }, [tasks]);

  return (
    <div className="relative overflow-hidden w-full h-[600px] rounded-xl glass-panel bg-black/20">
      <svg className="absolute inset-0 w-full h-full pointer-events-none">
        {layoutedTasks.map((task) => {
          const deps = task.dependencies || task.dependsOn || [];
          return deps.map((depName) => {
            const parent = layoutedTasks.find(t => t.name === depName);
            if (!parent) return null;

            const parentX = parent.x;
            const parentY = parent.y;
            const childX = task.x;
            const childY = task.y;

            return (
              <path
                key={`${parent.name}-${task.name}`}
                d={`M ${parentX} ${parentY} C ${parentX + 100} ${parentY}, ${childX - 100} ${childY}, ${childX} ${childY}`}
                stroke="rgba(255,255,255,0.2)"
                strokeWidth={2}
                fill="none"
              />
            );
          });
        })}
      </svg>

      {layoutedTasks.map(task => (
        <div
          key={task.id || task.name}
          onClick={() => onNodeClick && onNodeClick(task)}
          className={`absolute w-48 p-3 rounded-lg shadow-lg cursor-pointer transition-all hover:scale-105 glass-panel flex flex-col gap-2 items-center text-center ${getNodeStyles(task.status)}`}
          style={{ left: task.x, top: task.y, transform: 'translate(-50%, -50%)' }}
        >
          <span className="text-white font-medium text-sm truncate w-full">{task.name}</span>
          <StatusBadge status={task.status} />
        </div>
      ))}
    </div>
  );
}
