import { useRef, type CSSProperties } from "react";
import {
  motion,
  useScroll,
  useSpring,
  useTransform,
} from "motion/react";
import {
  ArrowDown,
  Code2,
  FileUp,
  MessageSquare,
  Palette,
} from "lucide-react";
import "./transfer-tunnel.css";

const SIGNALS = ["cyan", "violet", "coral", "amber"] as const;

export type FeatureStationKind = "file" | "text" | "chat" | "board";

export function TransferTunnel() {
  const trackRef = useRef<HTMLDivElement>(null);
  const { scrollYProgress } = useScroll({
    target: trackRef,
    offset: ["start start", "end end"],
  });
  const progress = useSpring(scrollYProgress, {
    stiffness: 72,
    damping: 24,
    mass: 0.24,
  });

  const tunnelRotate = useTransform(progress, [0, 1], [0, 220]);
  const tunnelScale = useTransform(
    progress,
    [0, 0.3, 0.58, 1],
    [0.78, 1.08, 1.34, 0.88],
  );
  const tunnelDepth = useTransform(
    progress,
    [0, 0.5, 1],
    ["0px", "180px", "-40px"],
  );
  const packetX = useTransform(
    progress,
    [0, 0.28, 0.48, 0.72, 1],
    ["-36vw", "-14vw", "0vw", "14vw", "36vw"],
  );
  const packetScale = useTransform(
    progress,
    [0, 0.42, 0.52, 0.7, 1],
    [1, 0.72, 0.12, 0.72, 1],
  );
  const packetRotate = useTransform(progress, [0, 1], [-18, 22]);
  const packetOpacity = useTransform(
    progress,
    [0, 0.46, 0.5, 0.66, 0.72, 0.9, 1],
    [1, 1, 0, 0, 1, 1, 0],
  );
  const codeOpacity = useTransform(
    progress,
    [0.3, 0.46, 0.58, 0.68],
    [0, 0, 1, 0],
  );
  const codeScale = useTransform(
    progress,
    [0.38, 0.52, 0.62],
    [0.7, 1, 1.08],
  );
  const particleOpacity = useTransform(progress, [0.82, 0.94, 1], [0, 1, 0]);
  const particleScale = useTransform(progress, [0.82, 1], [0.3, 1.6]);

  const sendStage = useTransform(progress, [0, 0.04, 0.2, 0.28], [0, 1, 1, 0]);
  const codeStage = useTransform(progress, [0.3, 0.38, 0.56, 0.64], [0, 1, 1, 0]);
  const receiveStage = useTransform(progress, [0.62, 0.7, 0.82, 0.88], [0, 1, 1, 0]);
  const disappearStage = useTransform(progress, [0.84, 0.91, 0.99, 1], [0, 1, 1, 0]);
  const scrollCueOpacity = useTransform(progress, [0, 0.08], [1, 0]);

  return (
    <div ref={trackRef} className="hefimer-tunnel-track">
      <div className="hefimer-tunnel-sticky">
        <div className="hefimer-tunnel-stage" aria-hidden="true">
          <div className="hefimer-tunnel-horizon" />
          <div className="hefimer-tunnel-signals">
            {SIGNALS.map((signal) => (
              <span key={signal} className={`hefimer-signal hefimer-signal--${signal}`} />
            ))}
          </div>

          <motion.div
            className="hefimer-tunnel-rings"
            style={{
              x: "-50%",
              y: "-50%",
              rotate: tunnelRotate,
              scale: tunnelScale,
              translateZ: tunnelDepth,
            }}
          >
            {Array.from({ length: 11 }).map((_, index) => (
              <span
                key={index}
                className="hefimer-depth-ring"
                style={{ "--ring-index": index } as CSSProperties}
              />
            ))}
          </motion.div>

          <motion.div
            className="hefimer-transfer-packet"
            style={{
              x: packetX,
              scale: packetScale,
              rotate: packetRotate,
              opacity: packetOpacity,
            }}
          >
            <span className="hefimer-packet-fold" />
            <FileUp size={22} strokeWidth={1.7} />
            <span>TRANSFER</span>
          </motion.div>

          <motion.div
            className="hefimer-code-cluster"
            style={{ opacity: codeOpacity, scale: codeScale }}
          >
            {Array.from({ length: 5 }).map((_, index) => (
              <motion.span
                key={index}
                initial={{ y: 16, opacity: 0 }}
                whileInView={{ y: 0, opacity: 1 }}
                transition={{ delay: index * 0.06 }}
              >
                •
              </motion.span>
            ))}
          </motion.div>

          <div className="hefimer-orbit-core">
            <motion.img
              src="/hefimer-orbit.svg"
              alt=""
              draggable={false}
              style={{ rotate: tunnelRotate }}
            />
            <span className="hefimer-core-light" />
          </div>

          <motion.div
            className="hefimer-particle-field"
            style={{ opacity: particleOpacity, scale: particleScale }}
          >
            {Array.from({ length: 24 }).map((_, index) => (
              <span
                key={index}
                style={{ "--particle-index": index } as CSSProperties}
              />
            ))}
          </motion.div>
        </div>

        <div className="hefimer-stage-copy">
          <motion.div style={{ opacity: sendStage }}>
            <span>SEND</span>
            <strong>Choose what moves.</strong>
            <p>File, text, chat, or board.</p>
          </motion.div>
          <motion.div style={{ opacity: codeStage }}>
            <span>5-DIGIT CODE</span>
            <strong>Access becomes simple.</strong>
            <p>Share the code. No account required.</p>
          </motion.div>
          <motion.div style={{ opacity: receiveStage }}>
            <span>RECEIVE</span>
            <strong>Open what was sent.</strong>
            <p>Enter the code to retrieve the shared content.</p>
          </motion.div>
          <motion.div style={{ opacity: disappearStage }}>
            <span>DISAPPEAR</span>
            <strong>Nothing stays forever.</strong>
            <p>Shared content is removed when its timer ends.</p>
          </motion.div>
        </div>

        <motion.div className="hefimer-scroll-cue" style={{ opacity: scrollCueOpacity }}>
          <span>SCROLL TO TRANSFER</span>
          <ArrowDown size={15} />
        </motion.div>

        <div className="hefimer-tunnel-progress" aria-hidden="true">
          <motion.span style={{ scaleX: progress }} />
        </div>
      </div>
    </div>
  );
}

export function FeatureStationVisual({ kind }: { kind: FeatureStationKind }) {
  if (kind === "file") {
    return (
      <div className="hefimer-station-visual hefimer-file-visual" aria-hidden="true">
        <span className="hefimer-file-sheet hefimer-file-sheet--back" />
        <span className="hefimer-file-sheet hefimer-file-sheet--front">
          <FileUp size={25} />
          <i />
        </span>
        <span className="hefimer-file-track"><i /></span>
      </div>
    );
  }

  if (kind === "text") {
    return (
      <div className="hefimer-station-visual hefimer-text-visual" aria-hidden="true">
        <Code2 size={20} />
        <span><i /><i /><i /></span>
        <span><i /><i /></span>
        <span><i /><i /><i /><i /></span>
        <b />
      </div>
    );
  }

  if (kind === "chat") {
    return (
      <div className="hefimer-station-visual hefimer-chat-visual" aria-hidden="true">
        <span className="hefimer-chat-bubble hefimer-chat-bubble--one"><i /><i /><i /></span>
        <span className="hefimer-chat-bubble hefimer-chat-bubble--two"><MessageSquare size={18} /></span>
        <span className="hefimer-chat-pulse" />
      </div>
    );
  }

  return (
    <div className="hefimer-station-visual hefimer-board-visual" aria-hidden="true">
      <Palette size={20} />
      <svg viewBox="0 0 240 130" role="presentation">
        <motion.path
          d="M18 92 C50 22 76 116 112 58 S176 26 221 74"
          fill="none"
          pathLength="1"
          initial={{ pathLength: 0 }}
          whileInView={{ pathLength: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 1.8, ease: [0.16, 1, 0.3, 1] }}
        />
      </svg>
      <span className="hefimer-board-cursor" />
    </div>
  );
}
