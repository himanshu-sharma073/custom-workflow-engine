import React from "react";

/** Default scale for 24×24 viewBox glyphs in step nodes */
export const GLYPH_DEFAULT_SCALE = 0.78;

function glyphFrame(ix: number, iy: number, scale: number, children: React.ReactNode) {
  return (
    <g transform={`translate(${ix},${iy})`}>
      <g transform={`scale(${scale})`}>{children}</g>
    </g>
  );
}

/** Type glyph (Heroicons-inspired 24×24 viewBox). USER steps use Font Awesome in StepNode. */
export const NodeTypeGlyph = React.memo(function NodeTypeGlyph({
  type,
  ix,
  iy
}: {
  type: string;
  ix: number;
  iy: number;
}) {
  const upper = type.toUpperCase();
  const scale = GLYPH_DEFAULT_SCALE;
  const cls = "wg-node-type-glyph";

  switch (upper) {
    case "USER":
      return null;
    case "SYSTEM":
      return glyphFrame(ix, iy, scale, (
        <>
          <path
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinecap="round"
            strokeLinejoin="round"
            className={cls}
            d="M10.343 3.94c.09-.542.56-.94 1.11-.94h1.093c.55 0 1.02.398 1.11.94l.149.894c.07.424.384.764.78.93.398.164.855.142 1.205-.124l.757-.573a1.125 1.125 0 011.591.063l.773.775c.39.389.497.973.279 1.468l-.445 1.001c-.173.389-.086.839.229 1.132.317.297.764.379 1.155.239l.95-.348a1.125 1.125 0 011.369.628l.421 1.002c.237.563.068 1.212-.397 1.585l-.81.596a1.125 1.125 0 00-.094 1.772l.81.596c.465.373.634 1.022.397 1.585l-.421 1.002a1.125 1.125 0 01-1.369.627l-.95-.348a1.125 1.125 0 00-1.155.239c-.314.294-.402.743-.229 1.132l.445 1a1.125 1.125 0 01-.279 1.469l-.773.774a1.125 1.125 0 01-1.591.063l-.757-.573a1.125 1.125 0 00-1.205.124 1.082 1.082 0 00-.78.93l-.149.894c-.09.542-.56.94-1.11.94h-1.093c-.55 0-1.02-.398-1.11-.94l-.149-.894a1.082 1.082 0 00-.78-.93 1.125 1.125 0 00-1.205.124l-.757.574a1.125 1.125 0 01-1.591-.063l-.774-.774a1.125 1.125 0 01-.063-1.591l.573-.758a1.125 1.125 0 00-.124-1.205 1.082 1.082 0 00-.93-.78l-.895-.149A1.125 1.125 0 015.73 17.73l-.348-.95a1.125 1.125 0 01.239-1.155c.294-.314.743-.402 1.132-.229l1.001.445c.424.173.839-.086 1.132-.229.391-.317.478-.764.279-1.155l-.445-1.001a1.125 1.125 0 01.063-1.591l.573-.759a1.125 1.125 0 011.591-.063l.757.573c.397.169.849.157 1.205-.124.317-.297.478-.743.478-1.132l-.029-.896z"
          />
          <circle cx="12" cy="12" r="3" fill="none" stroke="currentColor" strokeWidth="1.6" className={cls} />
        </>
      ));
    case "API":
      return glyphFrame(ix, iy, scale, (
        <path
          fill="none"
          stroke="currentColor"
          strokeWidth="1.75"
          strokeLinecap="round"
          strokeLinejoin="round"
          className={cls}
          d="M21 15.546l-4.586-4.586M5.914 14.414L14.414 5.914a2 2 0 012.828 2.829L8.743 17.243a6 6 0 01-8.485 0 .75.75 0 011.06-1.061 4.59 4.59 0 006.062 6.063l11.657-11.657a5.999 5.999 0 10-9.071 8.086 4.591 4.591 0 01-8.086-9.073z"
        />
      ));
    case "DECISION":
      return glyphFrame(ix, iy, scale, (
        <path
          fill="none"
          stroke="currentColor"
          strokeWidth="1.75"
          strokeLinecap="round"
          strokeLinejoin="round"
          className={cls}
          d="M12 4v8m0 0l-4 5m4-5l4 5M8 21h8"
        />
      ));
    case "END":
      return glyphFrame(ix, iy, scale, (
        <>
          <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" strokeWidth="1.75" className={cls} />
          <circle cx="12" cy="12" r="4" fill="currentColor" className={cls} />
        </>
      ));
    case "DELAY":
      return glyphFrame(ix, iy, scale, (
        <>
          <path
            fill="none"
            stroke="currentColor"
            strokeWidth="1.75"
            strokeLinecap="round"
            className={cls}
            d="M12 6v6l4 2"
          />
          <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" strokeWidth="1.75" className={cls} />
        </>
      ));
    case "EVENT":
      return glyphFrame(ix, iy, scale, (
        <path
          fill="none"
          stroke="currentColor"
          strokeWidth="1.75"
          strokeLinecap="round"
          strokeLinejoin="round"
          className={cls}
          d="M6 17h12l-1-9a4 4 0 10-8 0l-1 9z M10 21a2 2 0 004 0"
        />
      ));
    case "SCRIPT":
      return glyphFrame(ix, iy, scale, (
        <path
          fill="none"
          stroke="currentColor"
          strokeWidth="1.75"
          strokeLinecap="round"
          strokeLinejoin="round"
          className={cls}
          d="M17.25 6.75L22 12l-4.75 5.25m-10.5 0L2 12l4.75-5.25M14.738 18.804L12 21.52l-.738-3.716M9.263 5.195L12 2.478l2.738 2.716"
        />
      ));
    case "SUB_WORKFLOW":
      return glyphFrame(ix, iy, scale, (
        <>
          <rect x="4" y="4" width="16" height="16" rx="2.5" fill="none" stroke="currentColor" strokeWidth="1.6" className={cls} />
          <rect x="8.5" y="8.5" width="11" height="11" rx="2" fill="none" stroke="currentColor" strokeWidth="1.4" className={cls} />
        </>
      ));
    default:
      return glyphFrame(ix, iy, scale, (
        <>
          <rect x="5" y="5" width="14" height="14" rx="2" fill="none" stroke="currentColor" strokeWidth="1.75" className={cls} />
          <path d="M10 14h7" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" className={cls} />
        </>
      ));
  }
});
