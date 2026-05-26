import React from 'react';
import PropTypes from 'prop-types';

/**
 * Displays the current challenge title, story, criteria, and progress.
 * Criteria are checked off based on the latest validation result.
 *
 * @param {Object}   props
 * @param {Object}   [props.challenge]         - Current challenge object
 * @param {number}   [props.totalChallenges]   - Total number of challenges
 * @param {number}   [props.challengeIndex]    - Zero-based index of current challenge
 * @param {Object}   [props.validationResult]  - Latest validation result from backend
 */
export const ChallengePanel = ({ challenge, totalChallenges, challengeIndex, validationResult, pendingChallenge, onNext }) => {
  if (!challenge) {
    return (
      <div className="challenge-panel challenge-panel--empty">
        <p>Loading challenge...</p>
      </div>
    );
  }

  const criteria = challenge.successCriteria || [];

  // Determine which criteria are met using unmetCriteria from the validation result.
  // A criterion at index i is met if its description is NOT in the unmetCriteria list.
  const unmet = new Set(validationResult?.unmetCriteria || []);
  const hasValidation = validationResult != null;

  const isCriterionMet = (c, i) => {
    if (!hasValidation) return false;
    if (validationResult.success) return true;
    // Match by type+name description used in ValidationService feedback
    const key = c.name ? `${c.type}: ${c.name}` : c.type;
    return !unmet.has(key);
  };

  const allMet = validationResult?.success === true;

  return (
    <div className={`challenge-panel${allMet ? ' challenge-panel--complete' : ''}`}>
      <div className="challenge-panel__progress">
        Challenge {challengeIndex + 1} / {totalChallenges} &nbsp;·&nbsp; Level {challenge.level}
      </div>

      <h2 className="challenge-panel__title">{challenge.title}</h2>

      {challenge.storyContext && (
        <div className="challenge-panel__story">
          <pre>{challenge.storyContext}</pre>
        </div>
      )}

      {challenge.description && (
        <p className="challenge-panel__description">{challenge.description}</p>
      )}

      {criteria.length > 0 && (
        <div className="challenge-panel__criteria">
          <h3>Objectives</h3>
          <ul>
            {criteria.map((c, i) => {
              const met = isCriterionMet(c, i);
              return (
                <li
                  key={i}
                  className={`challenge-panel__criterion${met ? ' challenge-panel__criterion--met' : ''}`}
                >
                  <span className="criterion-icon">{met ? '✓' : '○'}</span>
                  <span>{describeCriterion(c)}</span>
                </li>
              );
            })}
          </ul>
        </div>
      )}

      {allMet && (
        <div className="challenge-panel__complete-banner">
          🎉 Challenge Complete!
        </div>
      )}

      {pendingChallenge && (
        <button className="challenge-panel__next-btn" onClick={onNext}>
          Next Challenge →
        </button>
      )}
    </div>
  );
};

ChallengePanel.propTypes = {
  challenge: PropTypes.object,
  totalChallenges: PropTypes.number,
  challengeIndex: PropTypes.number,
  validationResult: PropTypes.object,
};

ChallengePanel.defaultProps = {
  totalChallenges: 15,
  challengeIndex: 0,
};

function describeCriterion(c) {
  const name = c.name ? `"${c.name}"` : '';
  switch (c.type) {
    case 'pod_exists':        return `Pod ${name} exists`;
    case 'pod_not_exists':    return `Pod ${name} has been deleted`;
    case 'pod_running':       return `Pod ${name} is Running`;
    case 'pod_status':        return `Pod ${name} is ${c.status || 'Running'}`;
    case 'pod_has_env_from_configmap': return `Pod ${name} uses ConfigMap "${c.configmap}"`;
    case 'deployment_exists': return `Deployment ${name} exists`;
    case 'deployment_ready':  return `Deployment ${name} has ${c.replicas || 'all'} replicas ready`;
    case 'deployment_image':  return `Deployment ${name} uses image "${c.image}"`;
    case 'service_exists':    return `Service ${name} exists`;
    case 'service_selector':  return `Service ${name} routes to "${c.selector}" pods`;
    case 'service_has_endpoints': return `Service ${name} has healthy endpoints`;
    case 'configmap_exists':  return `ConfigMap ${name} exists`;
    case 'secret_exists':     return `Secret ${name} exists`;
    case 'command_executed':  return `Run kubectl ${c.command} on a ${c.resource}`;
    default:                  return c.type + (c.name ? `: ${c.name}` : '');
  }
}

export default ChallengePanel;
