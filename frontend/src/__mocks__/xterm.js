export class Terminal {
  constructor() {
    this.onData = jest.fn();
    this._handlers = {};
  }
  loadAddon() {}
  open() {}
  write(text) { this._lastWrite = text; }
  writeln(text) { this._lastWriteln = text; }
  dispose() {}
  onData(cb) { this._onData = cb; }
}
